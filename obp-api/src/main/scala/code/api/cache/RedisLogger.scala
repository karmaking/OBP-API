package code.api.cache

import code.api.util.APIUtil
import redis.clients.jedis.Pipeline

import scala.collection.JavaConverters._



/**
 * Redis queue configuration per log level.
 */
case class RedisLogConfig(
                           queueName: String,
                           keepInCache: Int
                         )


/**
 * Simple Redis FIFO log writer.
 */
object RedisLogger {

  /**
   * Redis-backed logging utilities for OBP.
   */
  object LogLevel extends Enumeration {
    type LogLevel = Value
    val TRACE, DEBUG, INFO, WARNING, ERROR, ALL = Value

    /** Parse a string into LogLevel, defaulting to INFO if unknown */
    def valueOf(str: String): LogLevel = str.toUpperCase match {
      case "TRACE"   => TRACE
      case "DEBUG"   => DEBUG
      case "INFO"    => INFO
      case "WARN" | "WARNING" => WARNING
      case "ERROR"   => ERROR
      case "ALL"     => ALL
      case other     =>
        // fallback
        INFO
    }
  }


  // Define FIFO queues with max 1000 entries, configurable keepInCache
  val configs = Map(
    LogLevel.TRACE -> RedisLogConfig("trace_logs", APIUtil.getPropsAsIntValue("keep_n_trace_level_logs_in_cache", 0)),
    LogLevel.DEBUG -> RedisLogConfig("debug_logs", APIUtil.getPropsAsIntValue("keep_n_debug_level_logs_in_cache", 0)),
    LogLevel.INFO -> RedisLogConfig("info_logs", APIUtil.getPropsAsIntValue("keep_n_info_level_logs_in_cache", 0)),
    LogLevel.WARNING -> RedisLogConfig("warning_logs", APIUtil.getPropsAsIntValue("keep_n_warning_level_logs_in_cache", 0)),
    LogLevel.ERROR -> RedisLogConfig("error_logs", APIUtil.getPropsAsIntValue("keep_n_error_level_logs_in_cache", 0)),
    LogLevel.ALL -> RedisLogConfig("all_logs", APIUtil.getPropsAsIntValue("keep_n_all_level_logs_in_cache", 0))
  )


  /**
   * Write a log line to Redis FIFO queue.
   */
  def log(level: LogLevel.LogLevel, message: String): Unit = {
    if (Redis.jedisPool != null && configs != null) {
      val jedis = Redis.jedisPool.getResource
      try {
        val pipeline: Pipeline = jedis.pipelined()

        // Always log to the given level
        val levelConfig = configs(level)
        if (levelConfig.keepInCache > 0) {
          pipeline.lpush(levelConfig.queueName, message)
          pipeline.ltrim(levelConfig.queueName, 0, levelConfig.keepInCache - 1)
        }

        // Also log to ALL
        val allConfig = configs(LogLevel.ALL)
        if (allConfig.keepInCache > 0) {
          pipeline.lpush(allConfig.queueName, s"[$level] $message")
          pipeline.ltrim(allConfig.queueName, 0, allConfig.keepInCache - 1)
        }

        pipeline.sync()
      } finally {
        jedis.close()
      }
    }
  }


  case class LogEntry(level: String, message: String)
  case class LogTail(entries: List[LogEntry])

  /**
   * Read latest messages from Redis FIFO queue.
   */
  def tail(level: LogLevel.LogLevel): LogTail = {
    val config = configs(level)
    val jedis = Redis.jedisPool.getResource
    try {
      val rawLogs = jedis.lrange(config.queueName, 0, -1).asScala.toList.reverse

      // define regex once
      val pattern = """\[(\w+)\]\s+(.*)""".r

      val entries: List[LogEntry] = level match {
        case LogLevel.ALL =>
          rawLogs.flatMap {
            case pattern(lvl, msg) =>
              Some(LogEntry(lvl, msg)) // lvl is string like "DEBUG"
            case _ =>
              None
          }

        case other =>
          rawLogs.map(msg => LogEntry(other.toString, msg))
      }

      LogTail(entries)
    } finally {
      jedis.close()
    }
  }



}
