package code.api.util

import java.util.Date

import code.accountholders.AccountHolders
import code.api.Constant
import code.api.util.APIUtil.getPropsAsBoolValue
import code.api.util.ApiRole.{CanCreateAccount, CanCreateHistoricalTransactionAtBank}
import code.api.util.ErrorMessages.{ConsumerIsDisabled, UserIsDeleted, UsernameHasBeenLocked}
import code.api.util.RateLimitingJson.CallLimit
import code.bankconnectors.{Connector, LocalMappedConnectorInternal}
import code.entitlement.Entitlement
import code.loginattempts.LoginAttempt
import code.model.dataAccess.{AuthUser, MappedBankAccount}
import code.ratelimiting.{RateLimiting, RateLimitingDI}
import code.users.{UserInitActionProvider, Users}
import code.util.Helper.MdcLoggable
import code.views.Views
import com.openbankproject.commons.model.{AccountId, Bank, BankAccount, BankId, BankIdAccountId, User, ViewId}
import net.liftweb.common.{Box, Empty, Failure, Full}
import com.openbankproject.commons.ExecutionContext.Implicits.global
import net.liftweb.mapper.By

import scala.concurrent.Future


object AfterApiAuth extends MdcLoggable{
  /**
   * This function is used to execute actions after an user is authenticated via GUI
   * Types of authentication: GUI logon(OpenID Connect and OAuth1.0a)
   * @param authUser the authenticated user
   */
  def innerLoginUserInitAction(authUser: Box[AuthUser]) = {
    authUser.map { u => // Init actions
      logger.info("AfterApiAuth.innerLoginUserInitAction started successfully")
      sofitInitAction(u)
    } match {
        case Full(_) => logger.warn("AfterApiAuth.innerLoginUserInitAction completed successfully")
        case userInitActionFailure => logger.warn("AfterApiAuth.innerLoginUserInitAction: " + userInitActionFailure)
    }
  }
  /**
   * This function is used to execute actions after an user is authenticated via API
   * Types of authentication: Direct Login, OpenID Connect, OAuth1.0a, Direct Login, DAuth and Gateway Login
   */
  def outerLoginUserInitAction(result: Future[(Box[User], Option[CallContext])]): Future[(Box[User], Option[CallContext])] = {
    logger.debug("AfterApiAuth.outerLoginUserInitAction started successfully")
    for {
      (user: Box[User], cc) <- result
    } yield {
      user match {
        case Full(u) => // There is a user. Apply init actions
          val authUser: Box[AuthUser] = AuthUser.find(By(AuthUser.user, u.userPrimaryKey.value))
          innerLoginUserInitAction(authUser)
          (user, cc)
        case userInitActionFailure => // There is no user. Just forward the result.
          logger.debug("AfterApiAuth.outerLoginUserInitAction: " + userInitActionFailure)
          (user, cc)
      }
    }
  }  
  def checkUserIsDeletedOrLocked(res: Future[(Box[User], Option[CallContext])]): Future[(Box[User], Option[CallContext])] = {
    for {
      (user: Box[User], cc) <- res
    } yield {
      user match {
        case Full(u) => // There is a user. Check it.
          if (u.isDeleted.getOrElse(false)) {
            (Failure(UserIsDeleted), cc) // The user is DELETED.
          } else {
            LoginAttempt.userIsLocked(u.provider, u.name) match {
              case true => (Failure(UsernameHasBeenLocked), cc) // The user is LOCKED.
              case false => (user, cc) // All good
            }
          }
        case _ => // There is no user. Just forward the result.
          (user, cc)
      }
    }
  }
  def checkConsumerIsDisabled(res: Future[(Box[User], Option[CallContext])]): Future[(Box[User], Option[CallContext])] = {
    for {
      (user: Box[User], cc) <- res
    } yield {
      cc.map(_.consumer) match {
        case Some(Full(consumer)) if !consumer.isActive.get => // There is a consumer. Check it.
          (Failure(ConsumerIsDisabled), cc) // The Consumer is DISABLED.
        case _ => // There is no Consumer. Just forward the result.
          (user, cc)
      }
    }
  }

  /**
   * This block of code needs to update Call Context with Rate Limiting
   * Please note that first source is the table RateLimiting and second is the table Consumer
   */
  def checkRateLimiting(userIsLockedOrDeleted: Future[(Box[User], Option[CallContext])]): Future[(Box[User], Option[CallContext])] = {
    def getActiveLimits(consumerId: String): Future[List[RateLimiting]] = {
      RateLimitingUtil.useConsumerLimits match {
        case true => RateLimitingDI.rateLimiting.vend.getActiveCallLimitsByConsumerIdAtDate(consumerId, new Date())
        case false => Future(List.empty)
      }
    }
    for {
      (user, cc) <- userIsLockedOrDeleted
      consumer = cc.flatMap(_.consumer)
      version = cc.map(_.implementedInVersion).getOrElse("None") // Calculate apiVersion  in case of Rate Limiting
      operationId = cc.flatMap(_.operationId) // Unique Identifier of Dynamic Endpoints
      // Calculate apiName in case of Rate Limiting
      name = cc.flatMap(_.resourceDocument.map(_.partialFunctionName)) // 1st try: function name at resource doc
        .orElse(operationId) // 2nd try: In case of Dynamic Endpoint we can only use operationId
        .getOrElse("None") // Not found any unique identifier
      activeLimits <- getActiveLimits(consumer.map(_.consumerId.get).getOrElse(""))
    } yield {
      // Find the most specific rate limiting record for this request
      def findBestMatch(limits: List[RateLimiting], version: String, name: String): Option[RateLimiting] = {
        limits.find(rl => rl.apiVersion.contains(version) && rl.apiName.contains(name)) // 1st try: exact match
          .orElse(limits.find(rl => rl.apiName.contains(name))) // 2nd try: match by name only
          .orElse(limits.find(rl => rl.apiVersion.contains(version))) // 3rd try: match by version only
          .orElse(limits.find(rl => rl.apiName.isEmpty && rl.apiVersion.isEmpty)) // 4th try: general consumer limit
          .orElse(limits.headOption) // 5th try: any limit
      }

      val limit: Option[CallLimit] = if (activeLimits.nonEmpty) {
        findBestMatch(activeLimits, version, name) match {
          case Some(rl) => Some(CallLimit(
            Some(rl.rateLimitingId),
            rl.consumerId,
            rl.apiName,
            rl.apiVersion,
            rl.bankId,
            rl.perSecondCallLimit,
            rl.perMinuteCallLimit,
            rl.perHourCallLimit,
            rl.perDayCallLimit,
            rl.perWeekCallLimit,
            rl.perMonthCallLimit))
          case None => None
        }
      } else {
        // Fallback to consumer limits if no database records found
        consumer.map(c => CallLimit(
          None,
          c.consumerId.get,
          None,
          None,
          None,
          c.perSecondCallLimit.get,
          c.perMinuteCallLimit.get,
          c.perHourCallLimit.get,
          c.perDayCallLimit.get,
          c.perWeekCallLimit.get,
          c.perMonthCallLimit.get
        ))
      }
      (user, cc.map(_.copy(rateLimiting = limit)))
    }
  }
  
  private def sofitInitAction(user: AuthUser): Boolean = applyAction("sofit.logon_init_action.enabled") {
    def getOrCreateBankAccount(bank: Bank, accountId: String, label: String, accountType: String = ""): Box[BankAccount] = {
      MappedBankAccount.find(
        By(MappedBankAccount.bank, bank.bankId.value), 
        By(MappedBankAccount.theAccountId, accountId)
      ) match {
        case Full(bankAccount) => Full(bankAccount)
        case _ => 
          val account = LocalMappedConnectorInternal.createSandboxBankAccount(
            bankId = bank.bankId, accountId = AccountId(accountId), accountNumber = label + "-1",
            accountType = accountType, accountLabel =  s"$label",
            currency = "EUR", initialBalance = 0, accountHolderName = user.username.get,
            "",
            List.empty
          )
          if(account.isEmpty)  logger.warn(s"AfterApiAuth.sofitInitAction. Cannot create the $label: account for user." + user.firstName + " " + user.lastName)
          account
      }
    }
    
    Users.users.vend.getUserByResourceUserId(user.user.get) match {
      case Full(resourceUser) =>
        // Create a bank according to the rule: bankid = user.user_id
        val bankId = "user." + resourceUser.userId
        Connector.connector.vend.createOrUpdateBank(
          bankId = bankId,
          fullBankName = "user." + resourceUser.userId,
          shortBankName = "user." + resourceUser.userId,
          logoURL = "",
          websiteURL = "",
          swiftBIC = "",
          national_identifier = "",
          bankRoutingScheme = "USER_ID",
          bankRoutingAddress = resourceUser.userId,
          None
        ) match {
          case Full(bank) =>
            UserInitActionProvider.createOrUpdateInitAction(resourceUser.userId, "create-or-update-bank", bankId, true)
            // Add roles
            val addCanCreateAccount = Entitlement.entitlement.vend.getEntitlement(bank.bankId.value, resourceUser.userId, CanCreateAccount.toString()).or {
              Entitlement.entitlement.vend.addEntitlement(bank.bankId.value, resourceUser.userId, CanCreateAccount.toString())
            }.isDefined
            UserInitActionProvider.createOrUpdateInitAction(resourceUser.userId, "add-entitlement", CanCreateAccount.toString(), addCanCreateAccount)
            val addCanCreateHistoricalTransactionAtBank = Entitlement.entitlement.vend.getEntitlement(bank.bankId.value, resourceUser.userId, CanCreateHistoricalTransactionAtBank.toString()).or {
              Entitlement.entitlement.vend.addEntitlement(bank.bankId.value, resourceUser.userId, CanCreateHistoricalTransactionAtBank.toString())
            }.isDefined
            UserInitActionProvider.createOrUpdateInitAction(resourceUser.userId, "add-entitlement", CanCreateHistoricalTransactionAtBank.toString(), addCanCreateHistoricalTransactionAtBank)
            // Create Cash account
            val bankAccount = getOrCreateBankAccount(bank, "cash", "cash-flow").flatMap { account =>
              Views.views.vend.systemView(ViewId(Constant.SYSTEM_OWNER_VIEW_ID)).flatMap(view =>
                // Grant account access
                Views.views.vend.grantAccessToSystemView(bank.bankId, account.accountId, view, resourceUser)
              )
              // Create account holder
              AccountHolders.accountHolders.vend.getOrCreateAccountHolder(resourceUser, BankIdAccountId(bank.bankId, account.accountId))
            }.isDefined
            UserInitActionProvider.createOrUpdateInitAction(resourceUser.userId, "add-bank-account", "cache", bankAccount)
            addCanCreateAccount && addCanCreateHistoricalTransactionAtBank && bankAccount
          case _ =>
            logger.warn("AfterApiAuth.sofitInitAction. Cannot create the bank: user." + resourceUser.userId)
            UserInitActionProvider.createOrUpdateInitAction(resourceUser.userId, "createOrUpdateBank", bankId, false)
            false
        }
      case _ =>
        logger.warn("AfterApiAuth.sofitInitAction. Cannot find resource user by primary key: " + user.id.get)
        false
    }
  }

  private def applyAction(propsName: String)(blockOfCode: => Boolean): Boolean = {
    val enabled = getPropsAsBoolValue(propsName, false)
    if(enabled) blockOfCode else false
  }
  
}
