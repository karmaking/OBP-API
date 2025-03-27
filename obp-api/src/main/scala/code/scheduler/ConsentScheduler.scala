package code.scheduler

import code.actorsystem.ObpLookupSystem
import code.api.util.APIUtil
import code.consent.{ConsentStatus, MappedConsent}
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.util.ApiVersion
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
    startTask(interval = 60, () => unfinishedBerlinGroupConsents()) // Runs every 60 sec
    startTask(interval = 60, () => expiredBerlinGroupConsents()) // Runs every 60 sec
  }

  // Generic method to schedule a task
  private def startTask(interval: Long, task: () => Unit): Unit = {
    scheduler.schedule(
      initialDelay = Duration(interval, TimeUnit.SECONDS),
      interval = Duration(interval, TimeUnit.SECONDS),
      runnable = new Runnable {
        def run(): Unit = task()
      }
    )
  }

  // Calculate the timestamp 5 minutes ago
  private val someMinutesAgo: Date = {
    val minutes = APIUtil.getPropsAsIntValue("berlin_group_outdated_consents_interval", 5)
    val cal = Calendar.getInstance()
    cal.add(Calendar.MINUTE, -minutes)
    cal.getTime
  }

  private def unfinishedBerlinGroupConsents(): Unit = {
    Try {
      logger.debug("|---> Checking for outdated Berlin Group consents...")

      val outdatedConsents = MappedConsent.findAll(
        By(MappedConsent.mStatus, ConsentStatus.received.toString),
        By(MappedConsent.mApiStandard, ApiVersion.berlinGroupV13.apiStandard),
        By_<(MappedConsent.updatedAt, someMinutesAgo)
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

}
