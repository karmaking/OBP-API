package code.api

import code.api.util.{APIUtil, ErrorMessages}
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.util.ApiStandards


// Note: Import this with: import code.api.Constant._
object Constant extends MdcLoggable {
  logger.info("Instantiating Constants")

  final val directLoginHeaderName = "directlogin"
  
  object Pagination {
    final val offset = 0
    final val limit = 50
  }
  
  final val shortEndpointTimeoutInMillis = APIUtil.getPropsAsLongValue(nameOfProperty = "short_endpoint_timeout", 1L * 1000L)
  final val mediumEndpointTimeoutInMillis = APIUtil.getPropsAsLongValue(nameOfProperty = "medium_endpoint_timeout", 7L * 1000L)
  final val longEndpointTimeoutInMillis = APIUtil.getPropsAsLongValue(nameOfProperty = "long_endpoint_timeout", 55L * 1000L)
  
  final val h2DatabaseDefaultUrlValue = "jdbc:h2:mem:OBPTest_H2_v2.1.214;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=10"

  final val HostName = APIUtil.getPropsValue("hostname").openOrThrowException(ErrorMessages.HostnameNotSpecified)
  final val CONNECTOR = APIUtil.getPropsValue("connector")
  final val openidConnectEnabled = APIUtil.getPropsAsBoolValue("openid_connect.enabled", false)
  
  final val bgRemoveSignOfAmounts = APIUtil.getPropsAsBoolValue("BG_remove_sign_of_amounts", false)
  
  final val ApiInstanceId = {
    val apiInstanceIdFromProps = APIUtil.getPropsValue("api_instance_id")
    if(apiInstanceIdFromProps.isDefined){
      if(apiInstanceIdFromProps.head.endsWith("final")){
        apiInstanceIdFromProps.head
      }else{
        s"${apiInstanceIdFromProps.head}_${APIUtil.generateUUID()}"
      }    
    }else{
      APIUtil.generateUUID()
    }
  }
  
  final val localIdentityProvider = APIUtil.getPropsValue("local_identity_provider", HostName)
    
  final val mailUsersUserinfoSenderAddress = APIUtil.getPropsValue("mail.users.userinfo.sender.address", "sender-not-set")
  
  final val oauth2JwkSetUrl = APIUtil.getPropsValue(nameOfProperty = "oauth2.jwk_set.url")

  final val consumerDefaultLogoUrl = APIUtil.getPropsValue("consumer_default_logo_url")
  final val serverMode = APIUtil.getPropsValue("server_mode", "apis,portal")

  // This is the part before the version. Do not change this default!
  final val ApiPathZero = APIUtil.getPropsValue("apiPathZero", ApiStandards.obp.toString)
  
  final val CUSTOM_PUBLIC_VIEW_ID = "_public"
  final val SYSTEM_OWNER_VIEW_ID = "owner" // From this commit new owner views are system views
  final val SYSTEM_AUDITOR_VIEW_ID = "auditor"
  final val SYSTEM_ACCOUNTANT_VIEW_ID = "accountant"
  final val SYSTEM_FIREHOSE_VIEW_ID = "firehose"
  final val SYSTEM_STANDARD_VIEW_ID = "standard"
  final val SYSTEM_STAGE_ONE_VIEW_ID = "StageOne"
  final val SYSTEM_MANAGE_CUSTOM_VIEWS_VIEW_ID = "ManageCustomViews"
  // UK Open Banking
  final val SYSTEM_READ_ACCOUNTS_BASIC_VIEW_ID = "ReadAccountsBasic"
  final val SYSTEM_READ_ACCOUNTS_DETAIL_VIEW_ID = "ReadAccountsDetail"
  final val SYSTEM_READ_BALANCES_VIEW_ID = "ReadBalances"
  final val SYSTEM_READ_TRANSACTIONS_BASIC_VIEW_ID = "ReadTransactionsBasic"
  final val SYSTEM_READ_TRANSACTIONS_DEBITS_VIEW_ID = "ReadTransactionsDebits"
  final val SYSTEM_READ_TRANSACTIONS_DETAIL_VIEW_ID = "ReadTransactionsDetail"
  // Berlin Group
  final val SYSTEM_READ_ACCOUNTS_BERLIN_GROUP_VIEW_ID = "ReadAccountsBerlinGroup"
  final val SYSTEM_READ_BALANCES_BERLIN_GROUP_VIEW_ID = "ReadBalancesBerlinGroup"
  final val SYSTEM_READ_TRANSACTIONS_BERLIN_GROUP_VIEW_ID = "ReadTransactionsBerlinGroup"
  final val SYSTEM_INITIATE_PAYMENTS_BERLIN_GROUP_VIEW_ID = "InitiatePaymentsBerlinGroup"

  //This is used for the canRevokeAccessToViews_ and canGrantAccessToViews_ fields of SYSTEM_OWNER_VIEW_ID or SYSTEM_STANDARD_VIEW_ID.
  final val DEFAULT_CAN_GRANT_AND_REVOKE_ACCESS_TO_VIEWS = 
    SYSTEM_OWNER_VIEW_ID::
    SYSTEM_AUDITOR_VIEW_ID::
    SYSTEM_ACCOUNTANT_VIEW_ID::
    SYSTEM_FIREHOSE_VIEW_ID::
    SYSTEM_STANDARD_VIEW_ID::
    SYSTEM_STAGE_ONE_VIEW_ID::
    SYSTEM_MANAGE_CUSTOM_VIEWS_VIEW_ID::
    SYSTEM_READ_ACCOUNTS_BASIC_VIEW_ID::
    SYSTEM_READ_ACCOUNTS_DETAIL_VIEW_ID::
    SYSTEM_READ_BALANCES_VIEW_ID::
    SYSTEM_READ_TRANSACTIONS_BASIC_VIEW_ID::
    SYSTEM_READ_TRANSACTIONS_DEBITS_VIEW_ID::
    SYSTEM_READ_TRANSACTIONS_DETAIL_VIEW_ID::
    SYSTEM_READ_ACCOUNTS_BERLIN_GROUP_VIEW_ID::
    SYSTEM_READ_BALANCES_BERLIN_GROUP_VIEW_ID::
    SYSTEM_READ_TRANSACTIONS_BERLIN_GROUP_VIEW_ID :: 
      SYSTEM_INITIATE_PAYMENTS_BERLIN_GROUP_VIEW_ID :: Nil
  
  //We allow CBS side to generate views by getBankAccountsForUser.viewsToGenerate filed.
  // viewsToGenerate can be any views, and OBP will check the following list, to make sure only allowed views are generated
  // If some views are not allowed, obp just log it, do not throw exceptions.
  final val VIEWS_GENERATED_FROM_CBS_WHITE_LIST = 
    SYSTEM_OWNER_VIEW_ID::
    SYSTEM_ACCOUNTANT_VIEW_ID::
    SYSTEM_AUDITOR_VIEW_ID::
    SYSTEM_STAGE_ONE_VIEW_ID::
    SYSTEM_STANDARD_VIEW_ID::
    SYSTEM_MANAGE_CUSTOM_VIEWS_VIEW_ID::
    SYSTEM_READ_ACCOUNTS_BERLIN_GROUP_VIEW_ID::
    SYSTEM_READ_BALANCES_BERLIN_GROUP_VIEW_ID::
    SYSTEM_READ_TRANSACTIONS_BERLIN_GROUP_VIEW_ID ::
      SYSTEM_INITIATE_PAYMENTS_BERLIN_GROUP_VIEW_ID :: Nil

  //These are the default incoming and outgoing account ids. we will create both during the boot.scala.
  final val INCOMING_SETTLEMENT_ACCOUNT_ID = "OBP-INCOMING-SETTLEMENT-ACCOUNT"    
  final val OUTGOING_SETTLEMENT_ACCOUNT_ID = "OBP-OUTGOING-SETTLEMENT-ACCOUNT"    
  final val ALL_CONSUMERS = "ALL_CONSUMERS"  

  final val PARAM_LOCALE = "locale"
  final val PARAM_TIMESTAMP = "_timestamp_"


  final val LOCALISED_RESOURCE_DOC_PREFIX = "rd_localised_"
  final val DYNAMIC_RESOURCE_DOC_CACHE_KEY_PREFIX = "rd_dynamic_"
  final val STATIC_RESOURCE_DOC_CACHE_KEY_PREFIX = "rd_static_"
  final val ALL_RESOURCE_DOC_CACHE_KEY_PREFIX = "rd_all_"
  final val STATIC_SWAGGER_DOC_CACHE_KEY_PREFIX = "swagger_static_"
  final val CREATE_LOCALISED_RESOURCE_DOC_JSON_TTL: Int = APIUtil.getPropsValue(s"createLocalisedResourceDocJson.cache.ttl.seconds", "3600").toInt
  final val GET_DYNAMIC_RESOURCE_DOCS_TTL: Int = APIUtil.getPropsValue(s"dynamicResourceDocsObp.cache.ttl.seconds", "3600").toInt
  final val GET_STATIC_RESOURCE_DOCS_TTL: Int = APIUtil.getPropsValue(s"staticResourceDocsObp.cache.ttl.seconds", "3600").toInt
  final val SHOW_USED_CONNECTOR_METHODS: Boolean = APIUtil.getPropsAsBoolValue(s"show_used_connector_methods", false)
  
  final val CAN_SEE_TRANSACTION_OTHER_BANK_ACCOUNT = "canSeeTransactionOtherBankAccount"
  final val CAN_SEE_TRANSACTION_METADATA = "canSeeTransactionMetadata"
  final val CAN_SEE_TRANSACTION_DESCRIPTION = "canSeeTransactionDescription"
  final val CAN_SEE_TRANSACTION_AMOUNT = "canSeeTransactionAmount"
  final val CAN_SEE_TRANSACTION_TYPE = "canSeeTransactionType"
  final val CAN_SEE_TRANSACTION_CURRENCY = "canSeeTransactionCurrency"
  final val CAN_SEE_TRANSACTION_START_DATE = "canSeeTransactionStartDate"
  final val CAN_SEE_TRANSACTION_FINISH_DATE = "canSeeTransactionFinishDate"
  final val CAN_SEE_TRANSACTION_BALANCE = "canSeeTransactionBalance"
  final val CAN_SEE_COMMENTS = "canSeeComments"
  final val CAN_SEE_OWNER_COMMENT = "canSeeOwnerComment"
  final val CAN_SEE_TAGS = "canSeeTags"
  final val CAN_SEE_IMAGES = "canSeeImages"
  final val CAN_SEE_BANK_ACCOUNT_OWNERS = "canSeeBankAccountOwners"
  final val CAN_SEE_BANK_ACCOUNT_TYPE = "canSeeBankAccountType"
  final val CAN_SEE_BANK_ACCOUNT_BALANCE = "canSeeBankAccountBalance"
  final val CAN_QUERY_AVAILABLE_FUNDS = "canQueryAvailableFunds"
  final val CAN_SEE_BANK_ACCOUNT_LABEL = "canSeeBankAccountLabel"
  final val CAN_SEE_BANK_ACCOUNT_NATIONAL_IDENTIFIER = "canSeeBankAccountNationalIdentifier"
  final val CAN_SEE_BANK_ACCOUNT_SWIFT_BIC = "canSeeBankAccountSwift_bic"
  final val CAN_SEE_BANK_ACCOUNT_IBAN = "canSeeBankAccountIban"
  final val CAN_SEE_BANK_ACCOUNT_NUMBER = "canSeeBankAccountNumber"
  final val CAN_SEE_BANK_ACCOUNT_BANK_NAME = "canSeeBankAccountBankName"
  final val CAN_SEE_BANK_ACCOUNT_BANK_PERMALINK = "canSeeBankAccountBankPermalink"
  final val CAN_SEE_BANK_ROUTING_SCHEME = "canSeeBankRoutingScheme"
  final val CAN_SEE_BANK_ROUTING_ADDRESS = "canSeeBankRoutingAddress"
  final val CAN_SEE_BANK_ACCOUNT_ROUTING_SCHEME = "canSeeBankAccountRoutingScheme"
  final val CAN_SEE_BANK_ACCOUNT_ROUTING_ADDRESS = "canSeeBankAccountRoutingAddress"
  final val CAN_SEE_OTHER_ACCOUNT_NATIONAL_IDENTIFIER = "canSeeOtherAccountNationalIdentifier"
  final val CAN_SEE_OTHER_ACCOUNT_SWIFT_BIC = "canSeeOtherAccountSWIFT_BIC"
  final val CAN_SEE_OTHER_ACCOUNT_IBAN = "canSeeOtherAccountIBAN"
  final val CAN_SEE_OTHER_ACCOUNT_BANK_NAME = "canSeeOtherAccountBankName"
  final val CAN_SEE_OTHER_ACCOUNT_NUMBER = "canSeeOtherAccountNumber"
  final val CAN_SEE_OTHER_ACCOUNT_METADATA = "canSeeOtherAccountMetadata"
  final val CAN_SEE_OTHER_ACCOUNT_KIND = "canSeeOtherAccountKind"
  final val CAN_SEE_OTHER_BANK_ROUTING_SCHEME = "canSeeOtherBankRoutingScheme"
  final val CAN_SEE_OTHER_BANK_ROUTING_ADDRESS = "canSeeOtherBankRoutingAddress"
  final val CAN_SEE_OTHER_ACCOUNT_ROUTING_SCHEME = "canSeeOtherAccountRoutingScheme"
  final val CAN_SEE_OTHER_ACCOUNT_ROUTING_ADDRESS = "canSeeOtherAccountRoutingAddress"
  final val CAN_SEE_MORE_INFO = "canSeeMoreInfo"
  final val CAN_SEE_URL = "canSeeUrl"
  final val CAN_SEE_IMAGE_URL = "canSeeImageUrl"
  final val CAN_SEE_OPEN_CORPORATES_URL = "canSeeOpenCorporatesUrl"
  final val CAN_SEE_CORPORATE_LOCATION = "canSeeCorporateLocation"
  final val CAN_SEE_PHYSICAL_LOCATION = "canSeePhysicalLocation"
  final val CAN_SEE_PUBLIC_ALIAS = "canSeePublicAlias"
  final val CAN_SEE_PRIVATE_ALIAS = "canSeePrivateAlias"
  final val CAN_ADD_MORE_INFO = "canAddMoreInfo"
  final val CAN_ADD_URL = "canAddURL"
  final val CAN_ADD_IMAGE_URL = "canAddImageURL"
  final val CAN_ADD_OPEN_CORPORATES_URL = "canAddOpenCorporatesUrl"
  final val CAN_ADD_CORPORATE_LOCATION = "canAddCorporateLocation"
  final val CAN_ADD_PHYSICAL_LOCATION = "canAddPhysicalLocation"
  final val CAN_ADD_PUBLIC_ALIAS = "canAddPublicAlias"
  final val CAN_ADD_PRIVATE_ALIAS = "canAddPrivateAlias"
  final val CAN_ADD_COUNTERPARTY = "canAddCounterparty"
  final val CAN_GET_COUNTERPARTY = "canGetCounterparty"
  final val CAN_DELETE_COUNTERPARTY = "canDeleteCounterparty"
  final val CAN_DELETE_CORPORATE_LOCATION = "canDeleteCorporateLocation"
  final val CAN_DELETE_PHYSICAL_LOCATION = "canDeletePhysicalLocation"
  final val CAN_EDIT_OWNER_COMMENT = "canEditOwnerComment"
  final val CAN_ADD_COMMENT = "canAddComment"
  final val CAN_DELETE_COMMENT = "canDeleteComment"
  final val CAN_ADD_TAG = "canAddTag"
  final val CAN_DELETE_TAG = "canDeleteTag"
  final val CAN_ADD_IMAGE = "canAddImage"
  final val CAN_DELETE_IMAGE = "canDeleteImage"
  final val CAN_ADD_WHERE_TAG = "canAddWhereTag"
  final val CAN_SEE_WHERE_TAG = "canSeeWhereTag"
  final val CAN_DELETE_WHERE_TAG = "canDeleteWhereTag"
  final val CAN_ADD_TRANSACTION_REQUEST_TO_OWN_ACCOUNT = "canAddTransactionRequestToOwnAccount"
  final val CAN_ADD_TRANSACTION_REQUEST_TO_ANY_ACCOUNT = "canAddTransactionRequestToAnyAccount"
  final val CAN_SEE_BANK_ACCOUNT_CREDIT_LIMIT = "canSeeBankAccountCreditLimit"
  final val CAN_CREATE_DIRECT_DEBIT = "canCreateDirectDebit"
  final val CAN_CREATE_STANDING_ORDER = "canCreateStandingOrder"
  final val CAN_REVOKE_ACCESS_TO_CUSTOM_VIEWS = "canRevokeAccessToCustomViews"
  final val CAN_GRANT_ACCESS_TO_CUSTOM_VIEWS = "canGrantAccessToCustomViews"
  final val CAN_SEE_TRANSACTION_REQUESTS = "canSeeTransactionRequests"
  final val CAN_SEE_TRANSACTION_REQUEST_TYPES = "canSeeTransactionRequestTypes"
  final val CAN_SEE_AVAILABLE_VIEWS_FOR_BANK_ACCOUNT = "canSeeAvailableViewsForBankAccount"
  final val CAN_UPDATE_BANK_ACCOUNT_LABEL = "canUpdateBankAccountLabel"
  final val CAN_CREATE_CUSTOM_VIEW = "canCreateCustomView"
  final val CAN_DELETE_CUSTOM_VIEW = "canDeleteCustomView"
  final val CAN_UPDATE_CUSTOM_VIEW = "canUpdateCustomView"
  final val CAN_GET_CUSTOM_VIEW = "canGetCustomView"
  final val CAN_SEE_VIEWS_WITH_PERMISSIONS_FOR_ALL_USERS = "canSeeViewsWithPermissionsForAllUsers"
  final val CAN_SEE_VIEWS_WITH_PERMISSIONS_FOR_ONE_USER = "canSeeViewsWithPermissionsForOneUser"
  final val CAN_SEE_TRANSACTION_THIS_BANK_ACCOUNT = "canSeeTransactionThisBankAccount"
  final val CAN_SEE_TRANSACTION_STATUS = "canSeeTransactionStatus"
  final val CAN_SEE_BANK_ACCOUNT_CURRENCY = "canSeeBankAccountCurrency"
  final val CAN_ADD_TRANSACTION_REQUEST_TO_BENEFICIARY = "canAddTransactionRequestToBeneficiary"
  final val CAN_GRANT_ACCESS_TO_VIEWS = "canGrantAccessToViews"
  final val CAN_REVOKE_ACCESS_TO_VIEWS = "canRevokeAccessToViews"

  final val VIEW_PERMISSION_NAMES = List(
    CAN_SEE_TRANSACTION_OTHER_BANK_ACCOUNT,
    CAN_SEE_TRANSACTION_METADATA,
    CAN_SEE_TRANSACTION_DESCRIPTION,
    CAN_SEE_TRANSACTION_AMOUNT,
    CAN_SEE_TRANSACTION_TYPE,
    CAN_SEE_TRANSACTION_CURRENCY,
    CAN_SEE_TRANSACTION_START_DATE,
    CAN_SEE_TRANSACTION_FINISH_DATE,
    CAN_SEE_TRANSACTION_BALANCE,
    CAN_SEE_COMMENTS,
    CAN_SEE_OWNER_COMMENT,
    CAN_SEE_TAGS,
    CAN_SEE_IMAGES,
    CAN_SEE_BANK_ACCOUNT_OWNERS,
    CAN_SEE_BANK_ACCOUNT_TYPE,
    CAN_SEE_BANK_ACCOUNT_BALANCE,
    CAN_QUERY_AVAILABLE_FUNDS,
    CAN_SEE_BANK_ACCOUNT_LABEL,
    CAN_SEE_BANK_ACCOUNT_NATIONAL_IDENTIFIER,
    CAN_SEE_BANK_ACCOUNT_SWIFT_BIC,
    CAN_SEE_BANK_ACCOUNT_IBAN,
    CAN_SEE_BANK_ACCOUNT_NUMBER,
    CAN_SEE_BANK_ACCOUNT_BANK_NAME,
    CAN_SEE_BANK_ACCOUNT_BANK_PERMALINK,
    CAN_SEE_BANK_ROUTING_SCHEME,
    CAN_SEE_BANK_ROUTING_ADDRESS,
    CAN_SEE_BANK_ACCOUNT_ROUTING_SCHEME,
    CAN_SEE_BANK_ACCOUNT_ROUTING_ADDRESS,
    CAN_SEE_OTHER_ACCOUNT_NATIONAL_IDENTIFIER,
    CAN_SEE_OTHER_ACCOUNT_SWIFT_BIC,
    CAN_SEE_OTHER_ACCOUNT_IBAN,
    CAN_SEE_OTHER_ACCOUNT_BANK_NAME,
    CAN_SEE_OTHER_ACCOUNT_NUMBER,
    CAN_SEE_OTHER_ACCOUNT_METADATA,
    CAN_SEE_OTHER_ACCOUNT_KIND,
    CAN_SEE_OTHER_BANK_ROUTING_SCHEME,
    CAN_SEE_OTHER_BANK_ROUTING_ADDRESS,
    CAN_SEE_OTHER_ACCOUNT_ROUTING_SCHEME,
    CAN_SEE_OTHER_ACCOUNT_ROUTING_ADDRESS,
    CAN_SEE_MORE_INFO,
    CAN_SEE_URL,
    CAN_SEE_IMAGE_URL,
    CAN_SEE_OPEN_CORPORATES_URL,
    CAN_SEE_CORPORATE_LOCATION,
    CAN_SEE_PHYSICAL_LOCATION,
    CAN_SEE_PUBLIC_ALIAS,
    CAN_SEE_PRIVATE_ALIAS,
    CAN_ADD_MORE_INFO,
    CAN_ADD_URL,
    CAN_ADD_IMAGE_URL,
    CAN_ADD_OPEN_CORPORATES_URL,
    CAN_ADD_CORPORATE_LOCATION,
    CAN_ADD_PHYSICAL_LOCATION,
    CAN_ADD_PUBLIC_ALIAS,
    CAN_ADD_PRIVATE_ALIAS,
    CAN_ADD_COUNTERPARTY,
    CAN_GET_COUNTERPARTY,
    CAN_DELETE_COUNTERPARTY,
    CAN_DELETE_CORPORATE_LOCATION,
    CAN_DELETE_PHYSICAL_LOCATION,
    CAN_EDIT_OWNER_COMMENT,
    CAN_ADD_COMMENT,
    CAN_DELETE_COMMENT,
    CAN_ADD_TAG,
    CAN_DELETE_TAG,
    CAN_ADD_IMAGE,
    CAN_DELETE_IMAGE,
    CAN_ADD_WHERE_TAG,
    CAN_SEE_WHERE_TAG,
    CAN_DELETE_WHERE_TAG,
    CAN_ADD_TRANSACTION_REQUEST_TO_OWN_ACCOUNT,
    CAN_ADD_TRANSACTION_REQUEST_TO_ANY_ACCOUNT,
    CAN_SEE_BANK_ACCOUNT_CREDIT_LIMIT,
    CAN_CREATE_DIRECT_DEBIT,
    CAN_CREATE_STANDING_ORDER,
    CAN_REVOKE_ACCESS_TO_CUSTOM_VIEWS,
    CAN_GRANT_ACCESS_TO_CUSTOM_VIEWS,
    CAN_SEE_TRANSACTION_REQUESTS,
    CAN_SEE_TRANSACTION_REQUEST_TYPES,
    CAN_SEE_AVAILABLE_VIEWS_FOR_BANK_ACCOUNT,
    CAN_UPDATE_BANK_ACCOUNT_LABEL,
    CAN_CREATE_CUSTOM_VIEW,
    CAN_DELETE_CUSTOM_VIEW,
    CAN_UPDATE_CUSTOM_VIEW,
    CAN_GET_CUSTOM_VIEW,
    CAN_SEE_VIEWS_WITH_PERMISSIONS_FOR_ALL_USERS,
    CAN_SEE_VIEWS_WITH_PERMISSIONS_FOR_ONE_USER,
    CAN_SEE_TRANSACTION_THIS_BANK_ACCOUNT,
    CAN_SEE_TRANSACTION_STATUS,
    CAN_SEE_BANK_ACCOUNT_CURRENCY,
    CAN_ADD_TRANSACTION_REQUEST_TO_BENEFICIARY,
    CAN_GRANT_ACCESS_TO_VIEWS,
    CAN_REVOKE_ACCESS_TO_VIEWS,
  )
}


object CertificateConstants {
  final val BEGIN_CERT: String = "-----BEGIN CERTIFICATE-----"
  final val END_CERT: String = "-----END CERTIFICATE-----"
}
object PrivateKeyConstants {
  final val BEGIN_KEY: String = "-----BEGIN PRIVATE KEY-----"
  final val END_KEY: String = "-----END PRIVATE KEY-----"
}

object JedisMethod extends Enumeration {
  type JedisMethod = Value
  val GET, SET, EXISTS, DELETE, TTL, INCR, FLUSHDB= Value
}


object ChargePolicy extends Enumeration {
  type ChargePolicy = Value
  val SHARED, SENDER, RECEIVER = Value
}

object RequestHeader {
  final lazy val `Consumer-Key` = "Consumer-Key"
  @deprecated("Use Consent-JWT","11-03-2020")
  final lazy val `Consent-Id` = "Consent-Id"
  final lazy val `Consent-ID` = "Consent-ID" // Berlin Group
  final lazy val `Consent-JWT` = "Consent-JWT"
  final lazy val `PSD2-CERT` = "PSD2-CERT"
  final lazy val `If-None-Match` = "If-None-Match"

  final lazy val `PSU-Geo-Location` = "PSU-Geo-Location" // Berlin Group
  final lazy val `PSU-Device-Name` = "PSU-Device-Name" // Berlin Group
  final lazy val `PSU-Device-ID` = "PSU-Device-ID" // Berlin Group
  final lazy val `PSU-IP-Address` = "PSU-IP-Address" // Berlin Group
  final lazy val `X-Request-ID` = "X-Request-ID" // Berlin Group
  final lazy val `TPP-Redirect-URI` = "TPP-Redirect-URI" // Berlin Group
  final lazy val `TPP-Nok-Redirect-URI` = "TPP-Nok-Redirect-URI" // Redirect URI in case of an error.
  final lazy val Date = "Date" // Berlin Group
  // Headers to support the signature function of Berlin Group
  final lazy val Digest = "Digest" // Berlin Group
  final lazy val Signature = "Signature" // Berlin Group
  final lazy val `TPP-Signature-Certificate` = "TPP-Signature-Certificate" // Berlin Group

  /**
   * The If-Modified-Since request HTTP header makes the request conditional: 
   * the server sends back the requested resource, with a 200 status, 
   * only if it has been last modified after the given date. 
   * If the resource has not been modified since, the response is a 304 without any body; 
   * the Last-Modified response header of a previous request contains the date of last modification. 
   * Unlike If-Unmodified-Since, If-Modified-Since can only be used with a GET or HEAD.
   *
   * When used in combination with If-None-Match, it is ignored, unless the server doesn't support If-None-Match. 
   */
  final lazy val `If-Modified-Since` = "If-Modified-Since"
}
object ResponseHeader {
  final lazy val `ASPSP-SCA-Approach` = "ASPSP-SCA-Approach" // Berlin Group
  final lazy val `Correlation-Id` = "Correlation-Id"
  final lazy val `WWW-Authenticate` = "WWW-Authenticate"
  final lazy val ETag = "ETag"
  final lazy val `Cache-Control` = "Cache-Control"
  final lazy val Connection = "Connection"
}

object BerlinGroup extends Enumeration {
  object ScaStatus extends Enumeration{
    type ChargePolicy = Value
    val received, psuIdentified, psuAuthenticated, scaMethodSelected, started, finalised, failed, exempted = Value
  }
  object AuthenticationType extends Enumeration{
    type ChargePolicy = Value
//    - 'SMS_OTP': An SCA method, where an OTP linked to the transaction to be authorised is sent to the PSU through a SMS channel.
//      - 'CHIP_OTP': An SCA method, where an OTP is generated by a chip card, e.g. a TOP derived from an EMV cryptogram.
//      To contact the card, the PSU normally needs a (handheld) device.
//    With this device, the PSU either reads the challenging data through a visual interface like flickering or
//      the PSU types in the challenge through the device key pad.
//      The device then derives an OTP from the challenge data and displays the OTP to the PSU.
//      - 'PHOTO_OTP': An SCA method, where the challenge is a QR code or similar encoded visual data
//      which can be read in by a consumer device or specific mobile app.
//      The device resp. the specific app than derives an OTP from the visual challenge data and displays
//      the OTP to the PSU.
//      - 'PUSH_OTP': An OTP is pushed to a dedicated authentication APP and displayed to the PSU.
    val SMS_OTP, CHIP_OTP, PHOTO_OTP, PUSH_OTP = Value
  }
}

