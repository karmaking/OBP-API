package code.api.v6_0_0

import code.accountattribute.AccountAttributeX
import code.api.ObpApiFailure
import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON._
import code.api.util.APIUtil._
import code.api.util.ApiRole.{canDeleteRateLimiting, canReadCallLimits, canSetCallLimits}
import code.api.util.ApiTag._
import code.api.util.ErrorMessages.{$UserNotLoggedIn, InvalidDateFormat, InvalidJsonFormat, UnknownError, _}
import code.api.util.FutureUtil.EndpointContext
import code.api.util.NewStyle.HttpCode
import code.api.util.{NewStyle, RateLimitingUtil}
import code.api.v3_0_0.JSONFactory300
import code.api.v6_0_0.JSONFactory600.{createActiveCallLimitsJsonV600, createCallLimitJsonV600, createCurrentUsageJson}
import code.bankconnectors.LocalMappedConnectorInternal
import code.bankconnectors.LocalMappedConnectorInternal._
import code.entitlement.Entitlement
import code.ratelimiting.RateLimitingDI
import code.views.Views
import com.github.dwickern.macros.NameOf.nameOf
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model._
import com.openbankproject.commons.util.{ApiVersion, ScannedApiVersion}
import net.liftweb.common.Full
import net.liftweb.http.rest.RestHelper

import java.text.SimpleDateFormat
import scala.collection.immutable.{List, Nil}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future


trait APIMethods600 {
  self: RestHelper =>

  val Implementations6_0_0 = new Implementations600()

  class Implementations600 {

    val implementedInApiVersion: ScannedApiVersion = ApiVersion.v6_0_0

    private val staticResourceDocs = ArrayBuffer[ResourceDoc]()
    def resourceDocs = staticResourceDocs 

    val apiRelations = ArrayBuffer[ApiRelation]()
    val codeContext = CodeContext(staticResourceDocs, apiRelations)


    staticResourceDocs += ResourceDoc(
      createTransactionRequestHold,
      implementedInApiVersion,
      nameOf(createTransactionRequestHold),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/owner/transaction-request-types/HOLD/transaction-requests",
      "Create Transaction Request (HOLD)",
      s"""
         |
         |Create a transaction request to move funds from the account to its Holding Account.
         |If the Holding Account does not exist, it will be created automatically.
         |
         |${transactionRequestGeneralText}
         |
       """.stripMargin,
      transactionRequestBodyHoldJsonV600,
      transactionRequestWithChargeJSON400,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        InsufficientAuthorisationToCreateTransactionRequest,
        InvalidTransactionRequestType,
        InvalidJsonFormat,
        NotPositiveAmount,
        InvalidTransactionRequestCurrency,
        TransactionDisabled,
        UnknownError
      ),
      List(apiTagTransactionRequest, apiTagPSD2PIS, apiTagPsd2)
    )

    lazy val createTransactionRequestHold: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: ViewId(viewId) :: "transaction-request-types" ::
        "HOLD" :: "transaction-requests" :: Nil JsonPost json -> _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          val transactionRequestType = TransactionRequestType("HOLD")
          LocalMappedConnectorInternal.createTransactionRequest(bankId, accountId, viewId , transactionRequestType, json)
    }

    // --- GET Holding Account by Parent ---
    staticResourceDocs += ResourceDoc(
      getHoldingAccountByReleaser,
      implementedInApiVersion,
      nameOf(getHoldingAccountByReleaser),
      "GET",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/VIEW_ID/holding-account",
      "Get Holding Account By Releaser",
      s"""
         |Return the Holding Account linked to the given releaser account via account attribute `RELEASER_ACCOUNT_ID`.
         |If multiple holding accounts exist, the first one will be returned.
         |
       """.stripMargin,
      EmptyBody,
      moderatedCoreAccountJsonV300,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        $UserNoPermissionAccessView,
        UnknownError
      ),
      List(apiTagAccount)
    )

    lazy val getHoldingAccountByReleaser: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: ViewId(viewId) :: "holding-account" :: Nil JsonGet _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          for {
            (user @Full(u), _, _, view, callContext) <- SS.userBankAccountView
            // Find accounts by attribute RELEASER_ACCOUNT_ID
            (accountIdsBox, callContext) <- AccountAttributeX.accountAttributeProvider.vend.getAccountIdsByParams(bankId, Map("RELEASER_ACCOUNT_ID" -> List(accountId.value))) map { ids => (ids, callContext) }
            accountIds = accountIdsBox.getOrElse(Nil)
            // load the first holding account
            holdingOpt <- {
              def firstHolding(ids: List[String]): Future[Option[BankAccount]] = ids match {
                case Nil => Future.successful(None)
                case id :: tail =>
                  NewStyle.function.getBankAccount(bankId, AccountId(id), callContext).flatMap { case (acc, cc) =>
                    if (acc.accountType == "HOLDING") Future.successful(Some(acc)) else firstHolding(tail)
                  }
              }
              firstHolding(accountIds)
            }
            holding <- NewStyle.function.tryons($BankAccountNotFound, 404, callContext) { holdingOpt.get }
            moderatedAccount <- NewStyle.function.moderatedBankAccountCore(holding, view, user, callContext)
          } yield {
            val core = JSONFactory300.createCoreBankAccountJSON(moderatedAccount)
            (core, HttpCode.`200`(callContext))
          }
    }
    
    staticResourceDocs += ResourceDoc(
      getCurrentCallsLimit,
      implementedInApiVersion,
      nameOf(getCurrentCallsLimit),
      "GET",
      "/management/consumers/CONSUMER_ID/consumer/current-usage",
      "Get Call Limits for a Consumer Usage",
      s"""
         |Get Call Limits for a Consumer Usage.
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      redisCallLimitJson,
      List(
        $UserNotLoggedIn,
        InvalidJsonFormat,
        InvalidConsumerId,
        ConsumerNotFoundByConsumerId,
        UserHasMissingRoles,
        UpdateConsumerError,
        UnknownError
      ),
      List(apiTagConsumer),
      Some(List(canReadCallLimits)))


    lazy val getCurrentCallsLimit: OBPEndpoint = {
      case "management" :: "consumers" :: consumerId :: "consumer" :: "current-usage" :: Nil JsonGet _ =>
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            _ <- NewStyle.function.getConsumerByConsumerId(consumerId, cc.callContext)
            currentUsage <- Future(RateLimitingUtil.consumerRateLimitState(consumerId).toList)
          } yield {
            (createCurrentUsageJson(currentUsage), HttpCode.`200`(cc.callContext))
          }
    }


    staticResourceDocs += ResourceDoc(
      createCallLimits,
      implementedInApiVersion,
      nameOf(createCallLimits),
      "POST",
      "/management/consumers/CONSUMER_ID/consumer/call-limits",
      "Create Call Limits for a Consumer",
      s"""
         |Create Call Limits for a Consumer
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      callLimitPostJsonV600,
      callLimitJsonV600,
      List(
        $UserNotLoggedIn,
        InvalidJsonFormat,
        InvalidConsumerId,
        ConsumerNotFoundByConsumerId,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagConsumer),
      Some(List(canSetCallLimits)))


    lazy val createCallLimits: OBPEndpoint = {
      case "management" :: "consumers" :: consumerId :: "consumer" :: "call-limits" :: Nil JsonPost json -> _ =>
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            _ <- NewStyle.function.hasEntitlement("", u.userId, canSetCallLimits, callContext)
            postJson <- NewStyle.function.tryons(s"$InvalidJsonFormat The Json body should be the $CallLimitPostJsonV600 ", 400, callContext) {
              json.extract[CallLimitPostJsonV600]
            }
            _ <- NewStyle.function.getConsumerByConsumerId(consumerId, callContext)
            rateLimiting <- RateLimitingDI.rateLimiting.vend.createOrUpdateConsumerCallLimits(
              consumerId,
              postJson.from_date,
              postJson.to_date,
              postJson.api_version,
              postJson.api_name,
              postJson.bank_id,
              Some(postJson.per_second_call_limit),
              Some(postJson.per_minute_call_limit),
              Some(postJson.per_hour_call_limit),
              Some(postJson.per_day_call_limit),
              Some(postJson.per_week_call_limit),
              Some(postJson.per_month_call_limit)
            )
          } yield {
            rateLimiting match {
              case Full(rateLimitingObj) => (createCallLimitJsonV600(rateLimitingObj), HttpCode.`201`(callContext))
              case _ => (UnknownError, HttpCode.`400`(callContext))
            }
          }
    }


    staticResourceDocs += ResourceDoc(
      deleteCallLimits,
      implementedInApiVersion,
      nameOf(deleteCallLimits),
      "DELETE",
      "/management/consumers/CONSUMER_ID/consumer/call-limits/RATE_LIMITING_ID",
      "Delete Call Limit by Rate Limiting ID",
      s"""
         |Delete a specific Call Limit by Rate Limiting ID
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      EmptyBody,
      List(
        $UserNotLoggedIn,
        InvalidConsumerId,
        ConsumerNotFoundByConsumerId,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagConsumer),
      Some(List(canDeleteRateLimiting)))


    lazy val deleteCallLimits: OBPEndpoint = {
      case "management" :: "consumers" :: consumerId :: "consumer" :: "call-limits" :: rateLimitingId :: Nil JsonDelete _ =>
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            _ <- NewStyle.function.hasEntitlement("", u.userId, canDeleteRateLimiting, callContext)
            _ <- NewStyle.function.getConsumerByConsumerId(consumerId, callContext)
            rateLimiting <- RateLimitingDI.rateLimiting.vend.getByRateLimitingId(rateLimitingId)
            _ <- rateLimiting match {
              case Full(rl) if rl.consumerId == consumerId => 
                Future.successful(Full(rl))
              case Full(_) => 
                Future.successful(ObpApiFailure(s"Rate limiting ID $rateLimitingId does not belong to consumer $consumerId", 400, callContext))
              case _ => 
                Future.successful(ObpApiFailure(s"Rate limiting ID $rateLimitingId not found", 404, callContext))
            }
            deleteResult <- RateLimitingDI.rateLimiting.vend.deleteByRateLimitingId(rateLimitingId)
          } yield {
            deleteResult match {
              case Full(true) => (EmptyBody, HttpCode.`204`(callContext))
              case _ => (UnknownError, HttpCode.`400`(callContext))
            }
          }
    }


    staticResourceDocs += ResourceDoc(
      getActiveCallLimitsAtDate,
      implementedInApiVersion,
      nameOf(getActiveCallLimitsAtDate),
      "GET",
      "/management/consumers/CONSUMER_ID/consumer/call-limits/active-at-date/DATE",
      "Get Active Call Limits at Date",
      s"""
         |Get the sum of call limits at a certain date time. This returns a SUM of all the records that span that time.
         |
         |Date format: YYYY-MM-DDTHH:MM:SSZ (e.g. 1099-12-31T23:00:00Z)
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      activeCallLimitsJsonV600,
      List(
        $UserNotLoggedIn,
        InvalidConsumerId,
        ConsumerNotFoundByConsumerId,
        UserHasMissingRoles,
        InvalidDateFormat,
        UnknownError
      ),
      List(apiTagConsumer),
      Some(List(canReadCallLimits)))


    lazy val getActiveCallLimitsAtDate: OBPEndpoint = {
      case "management" :: "consumers" :: consumerId :: "consumer" :: "call-limits" :: "active-at-date" :: dateString :: Nil JsonGet _ =>
        cc =>
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            _ <- NewStyle.function.hasEntitlement("", u.userId, canReadCallLimits, callContext)
            _ <- NewStyle.function.getConsumerByConsumerId(consumerId, callContext)
            date <- NewStyle.function.tryons(s"$InvalidDateFormat Current date format is: $dateString. Please use this format: YYYY-MM-DDTHH:MM:SSZ (e.g. 1099-12-31T23:00:00Z)", 400, callContext) {
              val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
              format.parse(dateString)
            }
            activeCallLimits <- RateLimitingDI.rateLimiting.vend.getActiveCallLimitsByConsumerIdAtDate(consumerId, date)
          } yield {
            (createActiveCallLimitsJsonV600(activeCallLimits, date), HttpCode.`200`(callContext))
          }
    }


    staticResourceDocs += ResourceDoc(
      getCurrentUser,
      implementedInApiVersion,
      nameOf(getCurrentUser), // TODO can we get this string from the val two lines above?
      "GET",
      "/users/current",
      "Get User (Current)",
      s"""Get the logged in user
         |
         |${userAuthenticationMessage(true)}
      """.stripMargin,
      EmptyBody,
      userJsonV300,
      List(UserNotLoggedIn, UnknownError),
      List(apiTagUser))

    lazy val getCurrentUser: OBPEndpoint = {
      case "users" :: "current" :: Nil JsonGet _ => {
        cc => {
          implicit val ec = EndpointContext(Some(cc))
          for {
            (Full(u), callContext) <- authenticatedAccess(cc)
            entitlements <- NewStyle.function.getEntitlementsByUserId(u.userId, callContext)
          } yield {
            val permissions: Option[Permission] = Views.views.vend.getPermissionForUser(u).toOption
            val currentUser = UserV600(u, entitlements, permissions)
            val onBehalfOfUser = if(cc.onBehalfOfUser.isDefined) {
              val user = cc.onBehalfOfUser.toOption.get
              val entitlements = Entitlement.entitlement.vend.getEntitlementsByUserId(user.userId).headOption.toList.flatten
              val permissions: Option[Permission] = Views.views.vend.getPermissionForUser(user).toOption
              Some(UserV600(user, entitlements, permissions))
            } else {
              None
            }
            (JSONFactory600.createUserInfoJSON(currentUser, onBehalfOfUser), HttpCode.`200`(callContext))
          }
        }
      }
    }

    staticResourceDocs += ResourceDoc(
      createTransactionRequestCardano,
      implementedInApiVersion,
      nameOf(createTransactionRequestCardano),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/owner/transaction-request-types/CARDANO/transaction-requests",
      "Create Transaction Request (CARDANO)",
      s"""
         |
         |For sandbox mode, it will use the Cardano Preprod Network.
         |The accountId can be the wallet_id for now, as it uses cardano-wallet in the backend.
         |
         |${transactionRequestGeneralText}
         |
       """.stripMargin,
      transactionRequestBodyCardanoJsonV600,
      transactionRequestWithChargeJSON400,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        InsufficientAuthorisationToCreateTransactionRequest,
        InvalidTransactionRequestType,
        InvalidJsonFormat,
        NotPositiveAmount,
        InvalidTransactionRequestCurrency,
        TransactionDisabled,
        UnknownError
      ),
      List(apiTagTransactionRequest, apiTagPSD2PIS, apiTagPsd2)
    )
    
    lazy val createTransactionRequestCardano: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: ViewId(viewId) :: "transaction-request-types" ::
        "CARDANO" :: "transaction-requests" :: Nil JsonPost json -> _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          val transactionRequestType = TransactionRequestType("CARDANO")
          LocalMappedConnectorInternal.createTransactionRequest(bankId, accountId, viewId , transactionRequestType, json)
    }

    staticResourceDocs += ResourceDoc(
      createTransactionRequestEthereumeSendTransaction,
      implementedInApiVersion,
      nameOf(createTransactionRequestEthereumeSendTransaction),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/owner/transaction-request-types/ETH_SEND_TRANSACTION/transaction-requests",
      "Create Transaction Request (ETH_SEND_TRANSACTION)",
      s"""
         |
         |Send ETH via Ethereum JSON-RPC.
         |AccountId should hold the 0x address for now.
         |
         |${transactionRequestGeneralText}
         |
       """.stripMargin,
      transactionRequestBodyEthereumJsonV600,
      transactionRequestWithChargeJSON400,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        InsufficientAuthorisationToCreateTransactionRequest,
        InvalidTransactionRequestType,
        InvalidJsonFormat,
        NotPositiveAmount,
        InvalidTransactionRequestCurrency,
        TransactionDisabled,
        UnknownError
      ),
      List(apiTagTransactionRequest, apiTagPSD2PIS, apiTagPsd2)
    )

    lazy val createTransactionRequestEthereumeSendTransaction: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: ViewId(viewId) :: "transaction-request-types" ::
        "ETH_SEND_TRANSACTION" :: "transaction-requests" :: Nil JsonPost json -> _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          val transactionRequestType = TransactionRequestType("ETH_SEND_TRANSACTION")
          LocalMappedConnectorInternal.createTransactionRequest(bankId, accountId, viewId , transactionRequestType, json)
    }
    staticResourceDocs += ResourceDoc(
      createTransactionRequestEthSendRawTransaction,
      implementedInApiVersion,
      nameOf(createTransactionRequestEthSendRawTransaction),
      "POST",
      "/banks/BANK_ID/accounts/ACCOUNT_ID/owner/transaction-request-types/ETH_SEND_RAW_TRANSACTION/transaction-requests",
      "CREATE TRANSACTION REQUEST (ETH_SEND_RAW_TRANSACTION )",
      s"""
         |
         |Send ETH via Ethereum JSON-RPC.
         |AccountId should hold the 0x address for now.
         |
         |${transactionRequestGeneralText}
         |
       """.stripMargin,
      transactionRequestBodyEthSendRawTransactionJsonV600,
      transactionRequestWithChargeJSON400,
      List(
        $UserNotLoggedIn,
        $BankNotFound,
        $BankAccountNotFound,
        InsufficientAuthorisationToCreateTransactionRequest,
        InvalidTransactionRequestType,
        InvalidJsonFormat,
        NotPositiveAmount,
        InvalidTransactionRequestCurrency,
        TransactionDisabled,
        UnknownError
      ),
      List(apiTagTransactionRequest, apiTagPSD2PIS, apiTagPsd2)
    )

    lazy val createTransactionRequestEthSendRawTransaction: OBPEndpoint = {
      case "banks" :: BankId(bankId) :: "accounts" :: AccountId(accountId) :: ViewId(viewId) :: "transaction-request-types" ::
        "ETH_SEND_RAW_TRANSACTION" :: "transaction-requests" :: Nil JsonPost json -> _ =>
        cc => implicit val ec = EndpointContext(Some(cc))
          val transactionRequestType = TransactionRequestType("ETH_SEND_RAW_TRANSACTION")
          LocalMappedConnectorInternal.createTransactionRequest(bankId, accountId, viewId , transactionRequestType, json)
    }
    
  }
}



object APIMethods600 extends RestHelper with APIMethods600 {
  lazy val newStyleEndpoints: List[(String, String)] = Implementations6_0_0.resourceDocs.map {
    rd => (rd.partialFunctionName, rd.implementedInApiVersion.toString())
  }.toList
}

