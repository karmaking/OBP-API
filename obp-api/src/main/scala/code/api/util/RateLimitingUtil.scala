package code.api.util

import code.api.{APIFailureNewStyle, JedisMethod}
import code.api.cache.Redis
import code.api.util.APIUtil.fullBoxOrException
import code.api.util.ErrorMessages.TooManyRequests
import code.api.util.RateLimitingJson.CallLimit
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.model.User
import net.liftweb.common.{Box, Empty}
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.time.temporal.{ChronoUnit, TemporalAdjusters}


import scala.collection.immutable
import scala.collection.immutable.{List, Nil}


object RateLimitingPeriod extends Enumeration {
  type LimitCallPeriod = Value
  val PER_SECOND, PER_MINUTE, PER_HOUR, PER_DAY, PER_WEEK, PER_MONTH, PER_YEAR = Value

  def toSeconds(period: LimitCallPeriod): Long = {
    period match {
      case PER_SECOND => 1
      case PER_MINUTE => 60
      case PER_HOUR   => 60 * 60
      case PER_DAY    => 60 * 60 * 24
      case PER_WEEK   => 60 * 60 * 24 * 7
      case PER_MONTH  => 60 * 60 * 24 * 30
      case PER_YEAR   => 60 * 60 * 24 * 365
    }
  }

  def toString(period: LimitCallPeriod): String = {
    period match {
      case PER_SECOND => "PER_SECOND"
      case PER_MINUTE => "PER_MINUTE"
      case PER_HOUR   => "PER_HOUR"
      case PER_DAY    => "PER_DAY"
      case PER_WEEK   => "PER_WEEK"
      case PER_MONTH  => "PER_MONTH"
      case PER_YEAR   => "PER_YEAR"
    }
  }
  def humanReadable(period: LimitCallPeriod): String = {
    period match {
      case PER_SECOND => "per second"
      case PER_MINUTE => "per minute"
      case PER_HOUR   => "per hour"
      case PER_DAY    => "per day"
      case PER_WEEK   => "per week"
      case PER_MONTH  => "per month"
      case PER_YEAR   => "per year"
    }
  }
}

object RateLimitingJson {
  case class CallLimit(
                  rate_limiting_id : Option[String],
                  consumer_id : String,
                  api_name : Option[String],
                  api_version : Option[String],
                  bank_id : Option[String],
                  per_second : Long,
                  per_minute : Long,
                  per_hour : Long,
                  per_day : Long,
                  per_week : Long,
                  per_month : Long
                )
}

object RateLimitingUtil extends MdcLoggable {
  import code.api.util.RateLimitingPeriod._
  
  def useConsumerLimits = APIUtil.getPropsAsBoolValue("use_consumer_limits", false)

  private def createUniqueKey(consumerKey: String, period: LimitCallPeriod) = consumerKey + RateLimitingPeriod.toString(period)
  
  private def createUniqueKeyWithId(rateLimitingId: Option[String], consumerKey: String, period: LimitCallPeriod) = {
    rateLimitingId match {
      case Some(id) => s"${consumerKey}_${id}_${RateLimitingPeriod.toString(period)}"
      case None => consumerKey + RateLimitingPeriod.toString(period)
    }
  }

  /**
   * Calculate the next calendar boundary for rate limiting periods
   * @param period the rate limiting period
   * @return Unix timestamp in seconds when the period should expire
   */
  private def getNextCalendarBoundary(period: LimitCallPeriod): Long = {
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    val nextBoundary = period match {
      case PER_SECOND => now.plus(1, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS)
      case PER_MINUTE => now.plus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)
      case PER_HOUR => now.plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS)
      case PER_DAY => now.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)
      case PER_WEEK => now.`with`(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS)
      case PER_MONTH => now.`with`(TemporalAdjusters.firstDayOfNextMonth()).truncatedTo(ChronoUnit.DAYS)
      case PER_YEAR => now.`with`(TemporalAdjusters.firstDayOfNextYear()).truncatedTo(ChronoUnit.DAYS)
    }
    nextBoundary.toEpochSecond
  }

  /**
   * Determine if a period should use calendar boundaries or fixed TTL
   * @param period the rate limiting period
   * @return true if calendar boundaries should be used
   */
  private def shouldUseCalendarBoundary(period: LimitCallPeriod): Boolean = {
    period match {
      case PER_DAY | PER_WEEK | PER_MONTH | PER_YEAR => true
      case _ => false
    }
  }

  private def underConsumerLimitsWithId(rateLimitingId: Option[String], consumerKey: String, period: LimitCallPeriod, limit: Long): Boolean = {
    if (useConsumerLimits) {
      try {
        (limit) match {
          case l if l > 0 => // Redis is available and limit is set
            val key = createUniqueKeyWithId(rateLimitingId, consumerKey, period)
            val exists = Redis.use(JedisMethod.EXISTS,key).map(_.toBoolean).get
            exists match {
              case true =>
                val underLimit = Redis.use(JedisMethod.GET,key).get.toLong + 1 <= limit // +1 means we count the current call as well. We increment later i.e after successful call.
                underLimit
              case false => // In case that key does not exist we return successful result
                true
            }
          case _ =>
            // Rate Limiting for a Consumer <= 0 implies successful result
            // Or any other unhandled case implies successful result
            true
        }
      } catch {
        case e : Throwable =>
          logger.error(s"Redis issue: $e")
          true
      }
    } else {
      true // Rate Limiting disabled implies successful result
    }
  }

  private def underConsumerLimits(consumerKey: String, period: LimitCallPeriod, limit: Long): Boolean = {
    if (useConsumerLimits) {
      try {
        (limit) match {
          case l if l > 0 => // Redis is available and limit is set
            val key = createUniqueKey(consumerKey, period)
            val exists = Redis.use(JedisMethod.EXISTS,key).map(_.toBoolean).get
            exists match {
              case true =>
                val underLimit = Redis.use(JedisMethod.GET,key).get.toLong + 1 <= limit // +1 means we count the current call as well. We increment later i.e after successful call.
                underLimit
              case false => // In case that key does not exist we return successful result
                true
            }
          case _ =>
            // Rate Limiting for a Consumer <= 0 implies successful result
            // Or any other unhandled case implies successful result
            true
        }
      } catch {
        case e : Throwable =>
          logger.error(s"Redis issue: $e")
          true
      }
    } else {
      true // Rate Limiting disabled implies successful result
    }
  }

  private def incrementConsumerCountersWithId(rateLimitingId: Option[String], consumerKey: String, period: LimitCallPeriod, limit: Long): (Long, Long) = {
    if (useConsumerLimits) {
      try {
        (limit) match {
          case -1 => // Limit is not set for the period - skip processing
            (-1, -1)
          case _ => // Redis is available and limit is set
            val key = createUniqueKeyWithId(rateLimitingId, consumerKey, period)
            val ttl =  Redis.use(JedisMethod.TTL, key).get.toInt
            ttl match {
              case -2 => // if the Key does not exists, -2 is returned
                if (shouldUseCalendarBoundary(period)) {
                  // Use calendar boundary expiration
                  val expirationTimestamp = getNextCalendarBoundary(period)
                  Redis.use(JedisMethod.SET, key, None, Some("1"))
                  Redis.use(JedisMethod.EXPIREAT, key, Some(expirationTimestamp.toInt), None)
                  val remainingSeconds = (expirationTimestamp - System.currentTimeMillis() / 1000).toInt
                  (remainingSeconds, 1)
                } else {
                  // Use fixed TTL for shorter periods
                  val seconds = RateLimitingPeriod.toSeconds(period).toInt
                  Redis.use(JedisMethod.SET, key, Some(seconds), Some("1"))
                  (seconds, 1)
                }
              case _ => // otherwise increment the counter
                val cnt = Redis.use(JedisMethod.INCR,key).get.toInt
                (ttl, cnt)
            }
        }
      } catch {
        case e : Throwable =>
          logger.error(s"Redis issue: $e")
          (-1, -1)
      }
    } else {
      (-1, -1)
    }
  }

  private def incrementConsumerCounters(consumerKey: String, period: LimitCallPeriod, limit: Long): (Long, Long) = {
    if (useConsumerLimits) {
      try {
        (limit) match {
          case -1 => // Limit is not set for the period - skip processing
            (-1, -1)
          case _ => // Redis is available and limit is set
            val key = createUniqueKey(consumerKey, period)
            val ttl =  Redis.use(JedisMethod.TTL, key).get.toInt
            ttl match {
              case -2 => // if the Key does not exists, -2 is returned
                if (shouldUseCalendarBoundary(period)) {
                  // Use calendar boundary expiration
                  val expirationTimestamp = getNextCalendarBoundary(period)
                  Redis.use(JedisMethod.SET, key, None, Some("1"))
                  Redis.use(JedisMethod.EXPIREAT, key, Some(expirationTimestamp.toInt), None)
                  val remainingSeconds = (expirationTimestamp - System.currentTimeMillis() / 1000).toInt
                  (remainingSeconds, 1)
                } else {
                  // Use fixed TTL for shorter periods
                  val seconds = RateLimitingPeriod.toSeconds(period).toInt
                  Redis.use(JedisMethod.SET, key, Some(seconds), Some("1"))
                  (seconds, 1)
                }
              case _ => // otherwise increment the counter
                val cnt = Redis.use(JedisMethod.INCR,key).get.toInt
                (ttl, cnt)
            }
        }
      } catch {
        case e : Throwable =>
          logger.error(s"Redis issue: $e")
          (-1, -1)
      }
    } else {
      (-1, -1)
    }
  }

  private def ttl(consumerKey: String, period: LimitCallPeriod): Long = {
    val key = createUniqueKey(consumerKey, period)
    val ttl = Redis.use(JedisMethod.TTL, key).get.toInt
    ttl match {
      case -2 => // if the Key does not exists, -2 is returned
        0
      case _ => // otherwise increment the counter
        ttl
    }
  }

  private def ttlWithId(rateLimitingId: Option[String], consumerKey: String, period: LimitCallPeriod): Long = {
    val key = createUniqueKeyWithId(rateLimitingId, consumerKey, period)
    val ttl = Redis.use(JedisMethod.TTL, key).get.toInt
    ttl match {
      case -2 => // if the Key does not exists, -2 is returned
        0
      case _ => // otherwise increment the counter
        ttl
    }
  }



  def consumerRateLimitState(consumerKey: String): immutable.Seq[((Option[Long], Option[Long]), LimitCallPeriod)] = {
    import code.ratelimiting.RateLimitingDI
    import java.util.Date
    import scala.concurrent.Await
    import scala.concurrent.duration._

    def getAggregatedInfo(consumerKey: String, period: LimitCallPeriod): ((Option[Long], Option[Long]), LimitCallPeriod) = {
      try {
        // Get all active rate limiting records for this consumer
        val allActiveLimits = Await.result(
          RateLimitingDI.rateLimiting.vend.getActiveCallLimitsByConsumerIdAtDate(consumerKey, new Date()), 
          5.seconds
        )

        // Collect all Redis keys for this specific period across all rate limiting records
        val allKeys = allActiveLimits.map { rateLimitRecord =>
          createUniqueKeyWithId(Some(rateLimitRecord.rateLimitingId), consumerKey, period)
        } :+ createUniqueKey(consumerKey, period) // Also include legacy key format

        // Debug logging for troubleshooting
        logger.debug(s"Getting aggregated info for period: $period, consumer: $consumerKey")
        logger.debug(s"All keys for this period: $allKeys")

        // Get values and TTLs for all keys for this specific period
        val allValues = allKeys.flatMap { key =>
          try {
            val valueOpt = Redis.use(JedisMethod.GET, key).map(_.toLong)
            logger.debug(s"Key: $key, Value: $valueOpt")
            valueOpt
          } catch {
            case _: Throwable => None
          }
        }

        val allTTLs = allKeys.flatMap { key =>
          try {
            val ttlOpt = Redis.use(JedisMethod.TTL, key).map(_.toLong)
            val filteredTtl = ttlOpt.filter(_ > -2) // Filter out non-existent keys (-2)
            
            // Additional bounds checking - TTL should not exceed the period's maximum
            val maxExpectedTTL = RateLimitingPeriod.toSeconds(period)
            val boundedTtl = filteredTtl.filter { ttl =>
              if (ttl > maxExpectedTTL) {
                logger.warn(s"Key $key has TTL ($ttl) exceeding expected maximum ($maxExpectedTTL) for period $period. Ignoring this TTL.")
                false
              } else {
                true
              }
            }
            
            logger.debug(s"Key: $key, TTL: $ttlOpt, Filtered TTL: $filteredTtl, Bounded TTL: $boundedTtl")
            boundedTtl
          } catch {
            case e: Throwable =>
              logger.debug(s"Error getting TTL for key $key: $e")
              None
          }
        }

        // Sum all values and take the maximum TTL for this specific period only
        val aggregatedValue = if (allValues.nonEmpty) Some(allValues.sum) else None
        val aggregatedTTL = if (allTTLs.nonEmpty) Some(allTTLs.max) else None

        logger.debug(s"Period: $period, Aggregated value: $aggregatedValue, Aggregated TTL: $aggregatedTTL")

        // Final validation: ensure TTL is reasonable for this period
        val validatedTTL = aggregatedTTL.filter { ttl =>
          val maxExpected = RateLimitingPeriod.toSeconds(period)
          if (ttl > maxExpected) {
            logger.warn(s"Aggregated TTL ($ttl) exceeds maximum for $period ($maxExpected). Clearing inconsistent keys.")
            // Clear potentially corrupted keys
            allKeys.foreach { key =>
              try {
                val keyTTL = Redis.use(JedisMethod.TTL, key).map(_.toLong).getOrElse(-1L)
                if (keyTTL > maxExpected) {
                  Redis.use(JedisMethod.DELETE, key)
                  logger.info(s"Deleted inconsistent key: $key (TTL was $keyTTL)")
                }
              } catch {
                case e: Throwable => logger.error(s"Error cleaning key $key: $e")
              }
            }
            false
          } else {
            true
          }
        }

        ((aggregatedValue, validatedTTL), period)
      } catch {
        case _: Throwable =>
          // Fallback to legacy behavior if there's any error
          val key = createUniqueKey(consumerKey, period)
          val ttlOpt: Option[Long] = Redis.use(JedisMethod.TTL, key).map(_.toLong)
          val valueOpt: Option[Long] = Redis.use(JedisMethod.GET, key).map(_.toLong)
          
          // Validate legacy key TTL as well
          val validatedLegacyTTL = ttlOpt.filter { ttl =>
            val maxExpected = RateLimitingPeriod.toSeconds(period)
            if (ttl > maxExpected) {
              logger.warn(s"Legacy key $key has invalid TTL ($ttl) for $period. Deleting.")
              Redis.use(JedisMethod.DELETE, key)
              false
            } else {
              true
            }
          }
          
          ((valueOpt, validatedLegacyTTL), period)
      }
    }

    getAggregatedInfo(consumerKey, RateLimitingPeriod.PER_SECOND) ::
    getAggregatedInfo(consumerKey, RateLimitingPeriod.PER_MINUTE) ::
    getAggregatedInfo(consumerKey, RateLimitingPeriod.PER_HOUR) ::
    getAggregatedInfo(consumerKey, RateLimitingPeriod.PER_DAY) ::
    getAggregatedInfo(consumerKey, RateLimitingPeriod.PER_WEEK) ::
    getAggregatedInfo(consumerKey, RateLimitingPeriod.PER_MONTH) ::
      Nil
  }

  /**
    * Diagnostic method to validate rate limiting logic and boundaries
    * This method helps debug issues with TTL calculation
    * @param consumerKey The consumer key to test
    * @param period The period to test
    * @return Debug information about the rate limiting logic
    */
  def debugRateLimitingLogic(consumerKey: String, period: LimitCallPeriod): String = {
    val debugInfo = StringBuilder.newBuilder
    debugInfo.append(s"=== Debug Rate Limiting Logic for $consumerKey, $period ===\n")
    
    // Test calendar boundary logic
    val usesCalendarBoundary = shouldUseCalendarBoundary(period)
    debugInfo.append(s"Uses calendar boundary: $usesCalendarBoundary\n")
    
    if (usesCalendarBoundary) {
      val boundary = getNextCalendarBoundary(period)
      val currentTime = System.currentTimeMillis() / 1000
      val remainingSeconds = boundary - currentTime
      debugInfo.append(s"Calendar boundary timestamp: $boundary\n")
      debugInfo.append(s"Current timestamp: $currentTime\n")
      debugInfo.append(s"Remaining seconds to boundary: $remainingSeconds\n")
    } else {
      val fixedTTL = RateLimitingPeriod.toSeconds(period)
      debugInfo.append(s"Fixed TTL seconds: $fixedTTL\n")
    }
    
    // Test key creation
    val legacyKey = createUniqueKey(consumerKey, period)
    debugInfo.append(s"Legacy key: $legacyKey\n")
    
    // Check what's actually in Redis
    try {
      val ttlValue = Redis.use(JedisMethod.TTL, legacyKey)
      val keyValue = Redis.use(JedisMethod.GET, legacyKey)
      debugInfo.append(s"Redis TTL for legacy key: $ttlValue\n")
      debugInfo.append(s"Redis value for legacy key: $keyValue\n")
    } catch {
      case e: Throwable =>
        debugInfo.append(s"Error accessing Redis: $e\n")
    }
    
    // Test the actual aggregation logic
    try {
      import code.ratelimiting.RateLimitingDI
      import java.util.Date
      import scala.concurrent.Await
      import scala.concurrent.duration._
      
      val allActiveLimits = Await.result(
        RateLimitingDI.rateLimiting.vend.getActiveCallLimitsByConsumerIdAtDate(consumerKey, new Date()), 
        5.seconds
      )
      
      debugInfo.append(s"Active rate limits count: ${allActiveLimits.length}\n")
      
      allActiveLimits.foreach { limit =>
        val keyWithId = createUniqueKeyWithId(Some(limit.rateLimitingId), consumerKey, period)
        debugInfo.append(s"Rate limit ID: ${limit.rateLimitingId}, Key: $keyWithId\n")
        
        try {
          val ttl = Redis.use(JedisMethod.TTL, keyWithId)
          val value = Redis.use(JedisMethod.GET, keyWithId)
          debugInfo.append(s"  TTL: $ttl, Value: $value\n")
        } catch {
          case e: Throwable =>
            debugInfo.append(s"  Error: $e\n")
        }
      }
    } catch {
      case e: Throwable =>
        debugInfo.append(s"Error getting active limits: $e\n")
    }
    
    debugInfo.toString()
  }

  /**
    * This function provides detailed rate limiting state for a consumer, showing information for each individual rate limiting record
    * @param consumerKey The consumer key to check
    * @return A sequence of detailed rate limiting information for each active record
    */
  def detailedConsumerRateLimitState(consumerKey: String): immutable.Seq[(String, String, immutable.Seq[((Option[Long], Option[Long]), LimitCallPeriod)])] = {
    import code.ratelimiting.RateLimitingDI
    import java.util.Date
    import scala.concurrent.Await
    import scala.concurrent.duration._

    try {
      // Get all active rate limiting records for this consumer
      val allActiveLimits = Await.result(
        RateLimitingDI.rateLimiting.vend.getActiveCallLimitsByConsumerIdAtDate(consumerKey, new Date()), 
        5.seconds
      )

      allActiveLimits.map { rateLimitRecord =>
        def getInfoForRecord(period: LimitCallPeriod): ((Option[Long], Option[Long]), LimitCallPeriod) = {
          val key = createUniqueKeyWithId(Some(rateLimitRecord.rateLimitingId), consumerKey, period)
          
          try {
            val ttlOpt: Option[Long] = Redis.use(JedisMethod.TTL, key).map(_.toLong).filter(_ > -2)
            val valueOpt: Option[Long] = Redis.use(JedisMethod.GET, key).map(_.toLong)
            ((valueOpt, ttlOpt), period)
          } catch {
            case _: Throwable => ((None, None), period)
          }
        }

        val recordInfo = 
          getInfoForRecord(RateLimitingPeriod.PER_SECOND) ::
          getInfoForRecord(RateLimitingPeriod.PER_MINUTE) ::
          getInfoForRecord(RateLimitingPeriod.PER_HOUR) ::
          getInfoForRecord(RateLimitingPeriod.PER_DAY) ::
          getInfoForRecord(RateLimitingPeriod.PER_WEEK) ::
          getInfoForRecord(RateLimitingPeriod.PER_MONTH) ::
          Nil

        val description = s"API: ${rateLimitRecord.apiName.getOrElse("*")} v${rateLimitRecord.apiVersion.getOrElse("*")} Bank: ${rateLimitRecord.bankId.getOrElse("*")}"
        
        (rateLimitRecord.rateLimitingId, description, recordInfo)
      }
    } catch {
      case _: Throwable => List.empty
    }
  }

  /**
    * This function checks rate limiting for a Consumer.
    * It will check rate limiting per minute, hour, day, week and month.
    * In case any of the above is hit an error is thrown.
    * In case two or more limits are hit rate limit with lower period has precedence regarding the error message.
    * @param userAndCallContext is a Tuple (Box[User], Option[CallContext]) provided from getUserAndSessionContextFuture function
    * @return a Tuple (Box[User], Option[CallContext]) enriched with rate limiting header or an error.
    */
  def underCallLimits(userAndCallContext: (Box[User], Option[CallContext])): (Box[User], Option[CallContext]) = {
    def perHourLimitAnonymous = APIUtil.getPropsAsIntValue("user_consumer_limit_anonymous_access", 1000)
    def composeMsgAuthorizedAccess(period: LimitCallPeriod, limit: Long): String = TooManyRequests + s" We only allow $limit requests ${RateLimitingPeriod.humanReadable(period)} for this Consumer."
    def composeMsgAnonymousAccess(period: LimitCallPeriod, limit: Long): String = TooManyRequests + s" We only allow $limit requests ${RateLimitingPeriod.humanReadable(period)} for anonymous access."

    def setXRateLimits(c: CallLimit, z: (Long, Long), period: LimitCallPeriod): Option[CallContext] = {
      val limit = period match {
        case PER_SECOND => c.per_second
        case PER_MINUTE => c.per_minute
        case PER_HOUR   => c.per_hour
        case PER_DAY    => c.per_day
        case PER_WEEK   => c.per_week
        case PER_MONTH  => c.per_month
        case PER_YEAR   => -1
      }
      userAndCallContext._2.map { cc: CallContext =>
        cc.copy(xRateLimitLimit = limit, xRateLimitReset = z._1, xRateLimitRemaining = limit - z._2)
      }
    }
    def setXRateLimitsAnonymous(id: String, z: (Long, Long), period: LimitCallPeriod): Option[CallContext] = {
      val limit = period match {
        case PER_HOUR   => perHourLimitAnonymous
        case _   => -1
      }
      userAndCallContext._2.map { cc: CallContext =>
        cc.copy(xRateLimitLimit = limit, xRateLimitReset = z._1, xRateLimitRemaining = limit - z._2)
      }
    }

    def exceededRateLimit(c: CallLimit, period: LimitCallPeriod): Option[CallContextLight] = {
      val remain = ttlWithId(c.rate_limiting_id, c.consumer_id, period)
      val limit = period match {
        case PER_SECOND => c.per_second
        case PER_MINUTE => c.per_minute
        case PER_HOUR   => c.per_hour
        case PER_DAY    => c.per_day
        case PER_WEEK   => c.per_week
        case PER_MONTH  => c.per_month
        case PER_YEAR   => -1
      }
      userAndCallContext._2.map { cc: CallContext =>
        cc.copy(xRateLimitLimit = limit, xRateLimitReset = remain, xRateLimitRemaining = 0).toLight
      }
    }

    def exceededRateLimitAnonymous(id: String, period: LimitCallPeriod): Option[CallContextLight] = {
      val remain = ttl(id, period)
      val limit = period match {
        case PER_HOUR   => perHourLimitAnonymous
        case _   => -1
      }
      userAndCallContext._2.map { cc: CallContext =>
        cc.copy(xRateLimitLimit = limit, xRateLimitReset = remain, xRateLimitRemaining = 0).toLight
      }
    }

    userAndCallContext._2 match {
      case Some(cc) =>
        cc.rateLimiting match {
          case Some(rl) => // Authorized access
            // Get all active rate limiting records for this consumer
            import code.ratelimiting.RateLimitingDI
            import java.util.Date
            import scala.concurrent.Await
            import scala.concurrent.duration._
            
            val consumerId = rl.consumer_id
            val allActiveLimits = try {
              Await.result(
                RateLimitingDI.rateLimiting.vend.getActiveCallLimitsByConsumerIdAtDate(consumerId, new Date()), 
                5.seconds
              )
            } catch {
              case _: Throwable => List.empty
            }
            
            // Group rate limiting records by period and combine limits
            val periodLimits = allActiveLimits.flatMap { rateLimitRecord =>
              List(
                (PER_SECOND, rateLimitRecord.perSecondCallLimit),
                (PER_MINUTE, rateLimitRecord.perMinuteCallLimit),
                (PER_HOUR, rateLimitRecord.perHourCallLimit),
                (PER_DAY, rateLimitRecord.perDayCallLimit),
                (PER_WEEK, rateLimitRecord.perWeekCallLimit),
                (PER_MONTH, rateLimitRecord.perMonthCallLimit)
              ).filter(_._2 > 0).map { case (period, limit) =>
                (period, rateLimitRecord, limit)
              }
            }.groupBy(_._1) // Group by period
            
            // Check combined usage for each period
            val combinedLimitChecks = periodLimits.map { case (period, recordsForPeriod) =>
              val combinedLimit = recordsForPeriod.map(_._3).sum // Sum all limits for this period
              val currentUsage = recordsForPeriod.map { case (_, rateLimitRecord, individualLimit) =>
                val rateLimitingKey = 
                  rateLimitRecord.consumerId + 
                  rateLimitRecord.apiName.getOrElse("") + 
                  rateLimitRecord.apiVersion.getOrElse("") + 
                  rateLimitRecord.bankId.getOrElse("")
                
                // Get current usage for this specific record
                try {
                  val key = createUniqueKeyWithId(Some(rateLimitRecord.rateLimitingId), rateLimitingKey, period)
                  Redis.use(JedisMethod.GET, key).map(_.toLong).getOrElse(0L)
                } catch {
                  case _: Throwable => 0L
                }
              }.sum
              
              (period, combinedLimit, currentUsage + 1, recordsForPeriod) // +1 for current request
            }
            
            // Check if any combined limit is exceeded
            val exceededCombinedLimit = combinedLimitChecks.find { case (_, combinedLimit, totalUsage, _) =>
              totalUsage > combinedLimit
            }
            
            exceededCombinedLimit match {
              case Some((period, combinedLimit, _, recordsForPeriod)) =>
                // Use the first record for error response
                val firstRecord = recordsForPeriod.head._2
                val callLimit = CallLimit(
                  Some(firstRecord.rateLimitingId),
                  firstRecord.consumerId,
                  firstRecord.apiName,
                  firstRecord.apiVersion,
                  firstRecord.bankId,
                  firstRecord.perSecondCallLimit,
                  firstRecord.perMinuteCallLimit,
                  firstRecord.perHourCallLimit,
                  firstRecord.perDayCallLimit,
                  firstRecord.perWeekCallLimit,
                  firstRecord.perMonthCallLimit
                )
                val errorMsg = composeMsgAuthorizedAccess(period, combinedLimit)
                (fullBoxOrException(Empty ~> APIFailureNewStyle(errorMsg, 429, exceededRateLimit(callLimit, period))), userAndCallContext._2)
              case None =>
                // Increment counters for all active limits
                val allIncrementCounters = periodLimits.flatMap { case (period, recordsForPeriod) =>
                  recordsForPeriod.map { case (_, rateLimitRecord, limit) =>
                    val rateLimitingKey = 
                      rateLimitRecord.consumerId + 
                      rateLimitRecord.apiName.getOrElse("") + 
                      rateLimitRecord.apiVersion.getOrElse("") + 
                      rateLimitRecord.bankId.getOrElse("")
                    (period, incrementConsumerCountersWithId(Some(rateLimitRecord.rateLimitingId), rateLimitingKey, period, limit))
                  }
                }
                
                // Find the first active counter to set rate limit headers
                allIncrementCounters.find(_._2._1 > 0) match {
                  case Some((period, counter)) =>
                    // Create a combined limit for headers
                    val combinedLimitForPeriod = periodLimits.get(period).map(_.map(_._3).sum).getOrElse(0L)
                    val modifiedRl = rl.copy(
                      per_second = if (period == PER_SECOND) combinedLimitForPeriod else rl.per_second,
                      per_minute = if (period == PER_MINUTE) combinedLimitForPeriod else rl.per_minute,
                      per_hour = if (period == PER_HOUR) combinedLimitForPeriod else rl.per_hour,
                      per_day = if (period == PER_DAY) combinedLimitForPeriod else rl.per_day,
                      per_week = if (period == PER_WEEK) combinedLimitForPeriod else rl.per_week,
                      per_month = if (period == PER_MONTH) combinedLimitForPeriod else rl.per_month
                    )
                    (userAndCallContext._1, setXRateLimits(modifiedRl, counter, period))
                  case None =>
                    (userAndCallContext._1, userAndCallContext._2)
                }
            }
          case None => // Anonymous access
            val consumerId = cc.ipAddress
            val checkLimits = List(
              underConsumerLimits(consumerId, PER_HOUR, perHourLimitAnonymous)
            )
            checkLimits match {
              case x1 :: Nil if x1 == false =>
                (fullBoxOrException(Empty ~> APIFailureNewStyle(composeMsgAnonymousAccess(PER_HOUR, perHourLimitAnonymous), 429, exceededRateLimitAnonymous(consumerId, PER_HOUR))), userAndCallContext._2)
              case _ =>
                val incrementCounters = List (
                  incrementConsumerCounters(consumerId, PER_HOUR, perHourLimitAnonymous),  // Responses other than the 429 status code MUST be stored by a cache.
                )
                incrementCounters match {
                  case x1 :: Nil if x1._1 > 0 =>
                    (userAndCallContext._1, setXRateLimitsAnonymous(consumerId, x1, PER_HOUR))
                  case _  =>
                    (userAndCallContext._1, userAndCallContext._2)
                }
            }
        }
      case _ => (userAndCallContext._1, userAndCallContext._2)
    }
  }


}