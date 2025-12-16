package code.abacrule

import code.api.util.{APIUtil, DynamicUtil}
import code.model.dataAccess.ResourceUser
import com.openbankproject.commons.model._
import net.liftweb.common.{Box, Empty, Failure, Full}
import net.liftweb.util.Helpers.tryo

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._
import scala.collection.concurrent

/**
 * ABAC Rule Engine for compiling and executing Attribute-Based Access Control rules
 */
object AbacRuleEngine {

  // Cache for compiled ABAC rule functions
  private val compiledRulesCache: concurrent.Map[String, Box[AbacRuleFunction]] = 
    new ConcurrentHashMap[String, Box[AbacRuleFunction]]().asScala

  /**
   * Type alias for compiled ABAC rule function
   * Parameters: User, Option[Bank], Option[Account], Option[Transaction], Option[Customer]
   * Returns: Boolean (true = allow access, false = deny access)
   */
  type AbacRuleFunction = (User, Option[Bank], Option[BankAccount], Option[Transaction], Option[Customer]) => Boolean

  /**
   * Compile an ABAC rule from Scala code
   * 
   * @param ruleId Unique identifier for the rule
   * @param ruleCode Scala code that defines the rule function
   * @return Box containing the compiled function or error
   */
  def compileRule(ruleId: String, ruleCode: String): Box[AbacRuleFunction] = {
    compiledRulesCache.get(ruleId) match {
      case Some(cachedFunction) => cachedFunction
      case None =>
        val compiledFunction = compileRuleInternal(ruleCode)
        compiledRulesCache.put(ruleId, compiledFunction)
        compiledFunction
    }
  }

  /**
   * Internal method to compile ABAC rule code
   */
  private def compileRuleInternal(ruleCode: String): Box[AbacRuleFunction] = {
    val fullCode = buildFullRuleCode(ruleCode)
    
    DynamicUtil.compileScalaCode[AbacRuleFunction](fullCode) match {
      case Full(func) => Full(func)
      case Failure(msg, exception, _) => 
        Failure(s"Failed to compile ABAC rule: $msg", exception, Empty)
      case Empty => 
        Failure("Failed to compile ABAC rule: Unknown error")
    }
  }

  /**
   * Build complete Scala code for compilation
   */
  private def buildFullRuleCode(ruleCode: String): String = {
    s"""
       |import com.openbankproject.commons.model._
       |import code.model.dataAccess.ResourceUser
       |import net.liftweb.common._
       |
       |// ABAC Rule Function
       |(user: User, bankOpt: Option[Bank], accountOpt: Option[BankAccount], transactionOpt: Option[Transaction], customerOpt: Option[Customer]) => {
       |  $ruleCode
       |}
       |""".stripMargin
  }

  /**
   * Execute an ABAC rule
   * 
   * @param ruleId The ID of the rule to execute
   * @param user The user requesting access
   * @param bankOpt Optional bank context
   * @param accountOpt Optional account context
   * @param transactionOpt Optional transaction context
   * @param customerOpt Optional customer context
   * @return Box[Boolean] - Full(true) if allowed, Full(false) if denied, Failure on error
   */
  def executeRule(
    ruleId: String,
    user: User,
    bankOpt: Option[Bank] = None,
    accountOpt: Option[BankAccount] = None,
    transactionOpt: Option[Transaction] = None,
    customerOpt: Option[Customer] = None
  ): Box[Boolean] = {
    for {
      rule <- MappedAbacRuleProvider.getAbacRuleById(ruleId)
      _ <- if (rule.isActive) Full(true) else Failure(s"ABAC Rule ${rule.ruleName} is not active")
      compiledFunc <- compileRule(ruleId, rule.ruleCode)
      result <- tryo {
        // Execute rule function directly
        // Note: Sandbox execution can be added later if needed
        compiledFunc(user, bankOpt, accountOpt, transactionOpt, customerOpt)
      }
    } yield result
  }

  /**
   * Execute multiple ABAC rules (AND logic - all must pass)
   * 
   * @param ruleIds List of rule IDs to execute
   * @param user The user requesting access
   * @param bankOpt Optional bank context
   * @param accountOpt Optional account context
   * @param transactionOpt Optional transaction context
   * @param customerOpt Optional customer context
   * @return Box[Boolean] - Full(true) if all rules pass, Full(false) if any rule fails
   */
  def executeRulesAnd(
    ruleIds: List[String],
    user: User,
    bankOpt: Option[Bank] = None,
    accountOpt: Option[BankAccount] = None,
    transactionOpt: Option[Transaction] = None,
    customerOpt: Option[Customer] = None
  ): Box[Boolean] = {
    if (ruleIds.isEmpty) {
      Full(true) // No rules means allow by default
    } else {
      val results = ruleIds.map { ruleId =>
        executeRule(ruleId, user, bankOpt, accountOpt, transactionOpt, customerOpt)
      }
      
      // Check if any rule failed
      results.find(_.exists(_ == false)) match {
        case Some(_) => Full(false) // At least one rule denied access
        case None =>
          // Check if all succeeded
          if (results.forall(_.isDefined)) {
            Full(true) // All rules passed
          } else {
            // At least one rule had an error
            val errors = results.collect { case Failure(msg, _, _) => msg }
            Failure(s"ABAC rule execution errors: ${errors.mkString("; ")}")
          }
      }
    }
  }

  /**
   * Execute multiple ABAC rules (OR logic - at least one must pass)
   * 
   * @param ruleIds List of rule IDs to execute
   * @param user The user requesting access
   * @param bankOpt Optional bank context
   * @param accountOpt Optional account context
   * @param transactionOpt Optional transaction context
   * @param customerOpt Optional customer context
   * @return Box[Boolean] - Full(true) if any rule passes, Full(false) if all rules fail
   */
  def executeRulesOr(
    ruleIds: List[String],
    user: User,
    bankOpt: Option[Bank] = None,
    accountOpt: Option[BankAccount] = None,
    transactionOpt: Option[Transaction] = None,
    customerOpt: Option[Customer] = None
  ): Box[Boolean] = {
    if (ruleIds.isEmpty) {
      Full(false) // No rules means deny by default for OR
    } else {
      val results = ruleIds.map { ruleId =>
        executeRule(ruleId, user, bankOpt, accountOpt, transactionOpt, customerOpt)
      }
      
      // Check if any rule passed
      results.find(_.exists(_ == true)) match {
        case Some(_) => Full(true) // At least one rule allowed access
        case None =>
          // All rules either failed or had errors
          if (results.exists(_.isDefined)) {
            Full(false) // All rules that executed denied access
          } else {
            // All rules had errors
            val errors = results.collect { case Failure(msg, _, _) => msg }
            Failure(s"All ABAC rules failed: ${errors.mkString("; ")}")
          }
      }
    }
  }

  /**
   * Validate ABAC rule code by attempting to compile it
   * 
   * @param ruleCode The Scala code to validate
   * @return Box[String] - Full("OK") if valid, Failure with error message if invalid
   */
  def validateRuleCode(ruleCode: String): Box[String] = {
    compileRuleInternal(ruleCode) match {
      case Full(_) => Full("ABAC rule code is valid")
      case Failure(msg, _, _) => Failure(s"Invalid ABAC rule code: $msg")
      case Empty => Failure("Failed to validate ABAC rule code")
    }
  }

  /**
   * Clear the compiled rules cache
   */
  def clearCache(): Unit = {
    compiledRulesCache.clear()
  }

  /**
   * Clear a specific rule from the cache
   */
  def clearRuleFromCache(ruleId: String): Unit = {
    compiledRulesCache.remove(ruleId)
  }

  /**
   * Get cache statistics
   */
  def getCacheStats(): Map[String, Any] = {
    Map(
      "cached_rules" -> compiledRulesCache.size,
      "rule_ids" -> compiledRulesCache.keys.toList
    )
  }
}