package code.api.v6_0_0

import code.abacrule.{AbacRuleEngine, MappedAbacRuleProvider}
import code.api.util.APIUtil._
import code.api.util.ApiRole._
import code.api.util.ApiTag._
import code.api.util.ErrorMessages._
import code.api.util.FutureUtil.EndpointContext
import code.api.util.NewStyle.HttpCode
import code.api.util.{APIUtil, CallContext, NewStyle}
import code.api.util.APIUtil.CodeContext
import code.api.v6_0_0.JSONFactory600._
import code.bankconnectors.Connector
import code.model.dataAccess.ResourceUser
import com.github.dwickern.macros.NameOf.nameOf
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model._
import com.openbankproject.commons.util.{ApiVersion, ScannedApiVersion}
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.http.rest.RestHelper

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

trait AbacRuleEndpoints {
  self: RestHelper =>

  val implementedInApiVersion: ScannedApiVersion
  val resourceDocs: ArrayBuffer[ResourceDoc]
  val staticResourceDocs: ArrayBuffer[ResourceDoc]
  val codeContext: CodeContext

  // Lazy initialization block - will be called when first endpoint is accessed
  private lazy val abacResourceDocsRegistered: Boolean = {
    registerAbacResourceDocs()
    true
  }

  private def registerAbacResourceDocs(): Unit = {
    // Create ABAC Rule
    staticResourceDocs += ResourceDoc(
      createAbacRule,
      implementedInApiVersion,
      nameOf(createAbacRule),
      "POST",
      "/management/abac-rules",
      "Create ABAC Rule",
      s"""Create a new ABAC (Attribute-Based Access Control) rule.
         |
         |ABAC rules are Scala functions that return a Boolean value indicating whether access should be granted.
         |
         |The rule function has the following signature:
         |```scala
         |(user: User, bankOpt: Option[Bank], accountOpt: Option[BankAccount], transactionOpt: Option[Transaction], customerOpt: Option[Customer]) => Boolean
         |```
         |
         |Example rule code:
         |```scala
         |// Allow access only if user email contains "admin"
         |user.emailAddress.contains("admin")
         |```
         |
         |```scala
         |// Allow access only to accounts with balance > 1000
         |accountOpt.exists(_.balance.toString.toDouble > 1000.0)
         |```
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      CreateAbacRuleJsonV600(
        rule_name = "admin_only",
        rule_code = """user.emailAddress.contains("admin")""",
        description = "Only allow access to users with admin email",
        is_active = true
      ),
      AbacRuleJsonV600(
        abac_rule_id = "abc123",
        rule_name = "admin_only",
        rule_code = """user.emailAddress.contains("admin")""",
        is_active = true,
        description = "Only allow access to users with admin email",
        created_by_user_id = "user123",
        updated_by_user_id = "user123"
      ),
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagABAC),
      Some(List(canCreateAbacRule))
    )

    // Get ABAC Rule by ID
    staticResourceDocs += ResourceDoc(
      getAbacRule,
      implementedInApiVersion,
      nameOf(getAbacRule),
      "GET",
      "/management/abac-rules/ABAC_RULE_ID",
      "Get ABAC Rule",
      s"""Get an ABAC rule by its ID.
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      AbacRuleJsonV600(
        abac_rule_id = "abc123",
        rule_name = "admin_only",
        rule_code = """user.emailAddress.contains("admin")""",
        is_active = true,
        description = "Only allow access to users with admin email",
        created_by_user_id = "user123",
        updated_by_user_id = "user123"
      ),
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagABAC),
      Some(List(canGetAbacRule))
    )

    // Get all ABAC Rules
    staticResourceDocs += ResourceDoc(
      getAbacRules,
      implementedInApiVersion,
      nameOf(getAbacRules),
      "GET",
      "/management/abac-rules",
      "Get ABAC Rules",
      s"""Get all ABAC rules.
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      AbacRulesJsonV600(
        abac_rules = List(
          AbacRuleJsonV600(
            abac_rule_id = "abc123",
            rule_name = "admin_only",
            rule_code = """user.emailAddress.contains("admin")""",
            is_active = true,
            description = "Only allow access to users with admin email",
            created_by_user_id = "user123",
            updated_by_user_id = "user123"
          )
        )
      ),
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagABAC),
      Some(List(canGetAbacRule))
    )

    // Update ABAC Rule
    staticResourceDocs += ResourceDoc(
      updateAbacRule,
      implementedInApiVersion,
      nameOf(updateAbacRule),
      "PUT",
      "/management/abac-rules/ABAC_RULE_ID",
      "Update ABAC Rule",
      s"""Update an existing ABAC rule.
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      UpdateAbacRuleJsonV600(
        rule_name = "admin_only_updated",
        rule_code = """user.emailAddress.contains("admin") && user.provider == "obp"""",
        description = "Only allow access to OBP admin users",
        is_active = true
      ),
      AbacRuleJsonV600(
        abac_rule_id = "abc123",
        rule_name = "admin_only_updated",
        rule_code = """user.emailAddress.contains("admin") && user.provider == "obp"""",
        is_active = true,
        description = "Only allow access to OBP admin users",
        created_by_user_id = "user123",
        updated_by_user_id = "user456"
      ),
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagABAC),
      Some(List(canUpdateAbacRule))
    )

    // Delete ABAC Rule
    staticResourceDocs += ResourceDoc(
      deleteAbacRule,
      implementedInApiVersion,
      nameOf(deleteAbacRule),
      "DELETE",
      "/management/abac-rules/ABAC_RULE_ID",
      "Delete ABAC Rule",
      s"""Delete an ABAC rule.
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      EmptyBody,
      EmptyBody,
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        UnknownError
      ),
      List(apiTagABAC),
      Some(List(canDeleteAbacRule))
    )

    // Execute ABAC Rule
    staticResourceDocs += ResourceDoc(
      executeAbacRule,
      implementedInApiVersion,
      nameOf(executeAbacRule),
      "POST",
      "/management/abac-rules/ABAC_RULE_ID/execute",
      "Execute ABAC Rule",
      s"""Execute an ABAC rule to test access control.
         |
         |This endpoint allows you to test an ABAC rule with specific context (bank, account, transaction, customer).
         |
         |${userAuthenticationMessage(true)}
         |
         |""".stripMargin,
      ExecuteAbacRuleJsonV600(
        bank_id = Some("gh.29.uk"),
        account_id = Some("8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0"),
        transaction_id = None,
        customer_id = None
      ),
      AbacRuleResultJsonV600(
        rule_id = "abc123",
        rule_name = "admin_only",
        result = true,
        message = "Access granted"
      ),
      List(
        UserNotLoggedIn,
        UserHasMissingRoles,
        InvalidJsonFormat,
        UnknownError
      ),
      List(apiTagABAC),
      Some(List(canExecuteAbacRule))
    )
  }

  lazy val createAbacRule: OBPEndpoint = {
    case "management" :: "abac-rules" :: Nil JsonPost json -> _ => {
      abacResourceDocsRegistered // Trigger lazy initialization
      cc => implicit val ec = EndpointContext(Some(cc))
        for {
          (Full(user), callContext) <- authenticatedAccess(cc)
          _ <- NewStyle.function.hasEntitlement("", user.userId, canCreateAbacRule, callContext)
          createJson <- NewStyle.function.tryons(s"$InvalidJsonFormat", 400, callContext) {
            json.extract[CreateAbacRuleJsonV600]
          }
          _ <- NewStyle.function.tryons(s"Rule name must not be empty", 400, callContext) {
            createJson.rule_name.nonEmpty
          }
          _ <- NewStyle.function.tryons(s"Rule code must not be empty", 400, callContext) {
            createJson.rule_code.nonEmpty
          }
          // Validate rule code by attempting to compile it
          _ <- Future {
            AbacRuleEngine.validateRuleCode(createJson.rule_code)
          } map {
            unboxFullOrFail(_, callContext, s"Invalid ABAC rule code", 400)
          }
          rule <- Future {
            MappedAbacRuleProvider.createAbacRule(
              ruleName = createJson.rule_name,
              ruleCode = createJson.rule_code,
              description = createJson.description,
              isActive = createJson.is_active,
              createdBy = user.userId
            )
          } map {
            unboxFullOrFail(_, callContext, s"Could not create ABAC rule", 400)
          }
        } yield {
          (createAbacRuleJsonV600(rule), HttpCode.`201`(callContext))
        }
    }
  }

  lazy val getAbacRule: OBPEndpoint = {
    case "management" :: "abac-rules" :: ruleId :: Nil JsonGet _ => {
      abacResourceDocsRegistered // Trigger lazy initialization
      cc => implicit val ec = EndpointContext(Some(cc))
        for {
          (Full(user), callContext) <- authenticatedAccess(cc)
          _ <- NewStyle.function.hasEntitlement("", user.userId, canGetAbacRule, callContext)
          rule <- Future {
            MappedAbacRuleProvider.getAbacRuleById(ruleId)
          } map {
            unboxFullOrFail(_, callContext, s"ABAC Rule not found with ID: $ruleId", 404)
          }
        } yield {
          (createAbacRuleJsonV600(rule), HttpCode.`200`(callContext))
        }
    }
  }

  lazy val getAbacRules: OBPEndpoint = {
    case "management" :: "abac-rules" :: Nil JsonGet _ => {
      abacResourceDocsRegistered // Trigger lazy initialization
      cc => implicit val ec = EndpointContext(Some(cc))
        for {
          (Full(user), callContext) <- authenticatedAccess(cc)
          _ <- NewStyle.function.hasEntitlement("", user.userId, canGetAbacRule, callContext)
          rules <- Future {
            MappedAbacRuleProvider.getAllAbacRules()
          }
        } yield {
          (createAbacRulesJsonV600(rules), HttpCode.`200`(callContext))
        }
    }
  }

  lazy val updateAbacRule: OBPEndpoint = {
    case "management" :: "abac-rules" :: ruleId :: Nil JsonPut json -> _ => {
      abacResourceDocsRegistered // Trigger lazy initialization
      cc => implicit val ec = EndpointContext(Some(cc))
        for {
          (Full(user), callContext) <- authenticatedAccess(cc)
          _ <- NewStyle.function.hasEntitlement("", user.userId, canUpdateAbacRule, callContext)
          updateJson <- NewStyle.function.tryons(s"$InvalidJsonFormat", 400, callContext) {
            json.extract[UpdateAbacRuleJsonV600]
          }
          // Validate rule code by attempting to compile it
          _ <- Future {
            AbacRuleEngine.validateRuleCode(updateJson.rule_code)
          } map {
            unboxFullOrFail(_, callContext, s"Invalid ABAC rule code", 400)
          }
          rule <- Future {
            MappedAbacRuleProvider.updateAbacRule(
              ruleId = ruleId,
              ruleName = updateJson.rule_name,
              ruleCode = updateJson.rule_code,
              description = updateJson.description,
              isActive = updateJson.is_active,
              updatedBy = user.userId
            )
          } map {
            unboxFullOrFail(_, callContext, s"Could not update ABAC rule with ID: $ruleId", 400)
          }
          // Clear rule from cache after update
          _ <- Future {
            AbacRuleEngine.clearRuleFromCache(ruleId)
          }
        } yield {
          (createAbacRuleJsonV600(rule), HttpCode.`200`(callContext))
        }
    }
  }

  lazy val deleteAbacRule: OBPEndpoint = {
    case "management" :: "abac-rules" :: ruleId :: Nil JsonDelete _ => {
      abacResourceDocsRegistered // Trigger lazy initialization
      cc => implicit val ec = EndpointContext(Some(cc))
        for {
          (Full(user), callContext) <- authenticatedAccess(cc)
          _ <- NewStyle.function.hasEntitlement("", user.userId, canDeleteAbacRule, callContext)
          deleted <- Future {
            MappedAbacRuleProvider.deleteAbacRule(ruleId)
          } map {
            unboxFullOrFail(_, callContext, s"Could not delete ABAC rule with ID: $ruleId", 400)
          }
          // Clear rule from cache after deletion
          _ <- Future {
            AbacRuleEngine.clearRuleFromCache(ruleId)
          }
        } yield {
          (Full(deleted), HttpCode.`204`(callContext))
        }
    }
  }

  lazy val executeAbacRule: OBPEndpoint = {
    case "management" :: "abac-rules" :: ruleId :: "execute" :: Nil JsonPost json -> _ => {
      abacResourceDocsRegistered // Trigger lazy initialization
      cc => implicit val ec = EndpointContext(Some(cc))
        for {
          (Full(user), callContext) <- authenticatedAccess(cc)
          _ <- NewStyle.function.hasEntitlement("", user.userId, canExecuteAbacRule, callContext)
          execJson <- NewStyle.function.tryons(s"$InvalidJsonFormat", 400, callContext) {
            json.extract[ExecuteAbacRuleJsonV600]
          }
          rule <- Future {
            MappedAbacRuleProvider.getAbacRuleById(ruleId)
          } map {
            unboxFullOrFail(_, callContext, s"ABAC Rule not found with ID: $ruleId", 404)
          }
          
          // Fetch context objects if IDs are provided
          bankOpt <- execJson.bank_id match {
            case Some(bankId) => NewStyle.function.getBank(BankId(bankId), callContext).map { case (bank, _) => Some(bank) }
            case None => Future.successful(None)
          }
          
          accountOpt <- execJson.account_id match {
            case Some(accountId) if execJson.bank_id.isDefined =>
              NewStyle.function.getBankAccount(BankId(execJson.bank_id.get), AccountId(accountId), callContext)
                .map { case (account, _) => Some(account) }
            case _ => Future.successful(None)
          }
          
          transactionOpt <- execJson.transaction_id match {
            case Some(transId) if execJson.bank_id.isDefined && execJson.account_id.isDefined =>
              NewStyle.function.getTransaction(
                BankId(execJson.bank_id.get),
                AccountId(execJson.account_id.get),
                TransactionId(transId),
                callContext
              ).map { case (transaction, _) => Some(transaction) }.recover { case _ => None }
            case _ => Future.successful(None)
          }
          
          customerOpt <- execJson.customer_id match {
            case Some(custId) if execJson.bank_id.isDefined =>
              NewStyle.function.getCustomerByCustomerId(custId, callContext)
                .map { case (customer, _) => Some(customer) }.recover { case _ => None }
            case _ => Future.successful(None)
          }
          
          // Execute the rule
          result <- Future {
            AbacRuleEngine.executeRule(
              ruleId = ruleId,
              user = user,
              bankOpt = bankOpt,
              accountOpt = accountOpt,
              transactionOpt = transactionOpt,
              customerOpt = customerOpt
            )
          } map {
            case Full(allowed) => 
              AbacRuleResultJsonV600(
                rule_id = ruleId,
                rule_name = rule.ruleName,
                result = allowed,
                message = if (allowed) "Access granted" else "Access denied"
              )
            case Failure(msg, _, _) =>
              AbacRuleResultJsonV600(
                rule_id = ruleId,
                rule_name = rule.ruleName,
                result = false,
                message = s"Execution error: $msg"
              )
            case Empty =>
              AbacRuleResultJsonV600(
                rule_id = ruleId,
                rule_name = rule.ruleName,
                result = false,
                message = "Execution failed"
              )
          }
        } yield {
          (result, HttpCode.`200`(callContext))
        }
    }
  }
}