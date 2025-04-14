package code.scheduler

import code.actorsystem.ObpLookupSystem
import code.api.util.APIUtil
import code.consent.{ConsentStatus, MappedConsent}
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.util.{ApiStandards, ApiVersion}
import net.liftweb.common.Full
import net.liftweb.mapper.{By, By_<}

import java.util.concurrent.TimeUnit
import java.util.{Calendar, Date}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


object ConsentScheduler extends MdcLoggable {

  private lazy val actorSystem = ObpLookupSystem.obpLookupSystem
  implicit lazy val executor = actorSystem.dispatcher
  private lazy val scheduler = actorSystem.scheduler

  // Starts multiple scheduled tasks with different intervals
  def startAll(): Unit = {
    var initialDelay = 0
    // Berlin Group
    APIUtil.getPropsAsIntValue("berlin_group_outdated_consents_interval_in_seconds") match {
      case Full(interval) if interval > 0 =>
        val time = APIUtil.getPropsAsIntValue("berlin_group_outdated_consents_time_in_seconds", 300)
        startTask(interval = interval, () => unfinishedBerlinGroupConsents(time)) // Runs periodically
        initialDelay = initialDelay + 10
      case _ =>
        logger.warn("|---> Skipping unfinishedBerlinGroupConsents task: berlin_group_outdated_consents_interval_in_seconds not set or invalid")
    }

    APIUtil.getPropsAsIntValue("berlin_group_expired_consents_interval_in_seconds") match {
      case Full(interval) if interval > 0 =>
        startTask(interval = interval, () => expiredBerlinGroupConsents(), initialDelay) // Delay for 10 seconds
        initialDelay = initialDelay + 10
      case _ =>
        logger.warn("|---> Skipping expiredBerlinGroupConsents task: berlin_group_expired_consents_interval_in_seconds not set or invalid")
    }

    // Open Bank Project
    APIUtil.getPropsAsIntValue("obp_expired_consents_interval_in_seconds") match {
      case Full(interval) if interval > 0 =>
        startTask(interval = interval, () => expiredObpConsents(), initialDelay) // Delay for 10 seconds
        initialDelay = initialDelay + 10
      case _ =>
        logger.warn("|---> Skipping expiredObpConsents task: obp_expired_consents_interval_in_seconds not set or invalid")
    }
  }

  // Generic method to schedule a task
  private def startTask(interval: Long, task: () => Unit, initialDelay: Long = 0): Unit = {
    scheduler.schedule(
      initialDelay = Duration(initialDelay, TimeUnit.SECONDS),
      interval = Duration(interval, TimeUnit.SECONDS),
      runnable = new Runnable {
        def run(): Unit = task()
      }
    )
  }

  // Calculate the timestamp 5 minutes ago
  private def someSecondsAgo(seconds: Int): Date = {
    val cal = Calendar.getInstance()
    cal.add(Calendar.SECOND, -seconds)
    cal.getTime
  }

  private def unfinishedBerlinGroupConsents(seconds: Int): Unit = {
    Try {
      logger.debug("|---> Checking for outdated Berlin Group consents...")

      val outdatedConsents = MappedConsent.findAll(
        By(MappedConsent.mStatus, ConsentStatus.received.toString),
        By(MappedConsent.mApiStandard, ApiVersion.berlinGroupV13.apiStandard),
        By_<(MappedConsent.updatedAt, someSecondsAgo(seconds))
      )

      logger.debug(s"|---> Found ${outdatedConsents.size} outdated consents")

      outdatedConsents.foreach { consent =>
        Try {
          consent.mStatus(ConsentStatus.rejected.toString).save
          logger.warn(s"|---> Changed status to ${ConsentStatus.rejected.toString} for consent ID: ${consent.id}")
        } match {
          case Failure(ex) => logger.error(s"Failed to update consent ID: ${consent.id}", ex)
          case Success(_) => // Already logged
        }
      }
    } match {
      case Failure(ex) => logger.error("Error in unfinishedBerlinGroupConsents!", ex)
      case Success(_) => logger.debug("|---> Task executed successfully")
    }
  }
  private def expiredBerlinGroupConsents(): Unit = {
    Try {
      logger.debug("|---> Checking for expired Berlin Group consents...")

      val expiredConsents = MappedConsent.findAll(
        By(MappedConsent.mStatus, ConsentStatus.valid.toString),
        By(MappedConsent.mApiStandard, ApiVersion.berlinGroupV13.apiStandard),
        By_<(MappedConsent.mValidUntil, new Date())
      )

      logger.debug(s"|---> Found ${expiredConsents.size} expired consents")

      expiredConsents.foreach { consent =>
        Try {
          consent.mStatus(ConsentStatus.expired.toString).save
          logger.warn(s"|---> Changed status to ${ConsentStatus.expired.toString} for consent ID: ${consent.id}")
        } match {
          case Failure(ex) => logger.error(s"Failed to update consent ID: ${consent.id}", ex)
          case Success(_) => // Already logged
        }
      }
    } match {
      case Failure(ex) => logger.error("Error in expiredBerlinGroupConsents!", ex)
      case Success(_) => logger.debug("|---> Task executed successfully")
    }
  }
  private def expiredObpConsents(): Unit = {
    Try {
      logger.debug("|---> Checking for expired OBP consents...")

      val expiredConsents = MappedConsent.findAll(
        By(MappedConsent.mStatus, ConsentStatus.ACCEPTED.toString),
        By(MappedConsent.mApiStandard, ApiStandards.obp.toString),
        By_<(MappedConsent.mValidUntil, new Date())
      )

      logger.debug(s"|---> Found ${expiredConsents.size} expired consents")

      expiredConsents.foreach { consent =>
        Try {
          consent.mStatus(ConsentStatus.EXPIRED.toString).save
          logger.warn(s"|---> Changed status to ${ConsentStatus.EXPIRED.toString} for consent ID: ${consent.id}")
        } match {
          case Failure(ex) => logger.error(s"Failed to update consent ID: ${consent.id}", ex)
          case Success(_) => // Already logged
        }
      }
    } match {
      case Failure(ex) => logger.error("Error in expiredObpConsents!", ex)
      case Success(_) => logger.debug("|---> Task executed successfully")
    }
  }

}
