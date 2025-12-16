package code.abacrule

import code.api.util.APIUtil
import com.openbankproject.commons.model._
import net.liftweb.common.Box
import net.liftweb.mapper._
import net.liftweb.util.Helpers.tryo

import java.util.Date

trait AbacRule {
  def abacRuleId: String
  def ruleName: String
  def ruleCode: String
  def isActive: Boolean
  def description: String
  def createdByUserId: String
  def updatedByUserId: String
}

class MappedAbacRule extends AbacRule with LongKeyedMapper[MappedAbacRule] with IdPK with CreatedUpdated {
  def getSingleton = MappedAbacRule

  object AbacRuleId extends MappedString(this, 255) {
    override def defaultValue = APIUtil.generateUUID()
    override def dbColumnName = "abac_rule_id"
  }
  object RuleName extends MappedString(this, 255) {
    override def dbColumnName = "rule_name"
  }
  object RuleCode extends MappedText(this) {
    override def dbColumnName = "rule_code"
  }
  object IsActive extends MappedBoolean(this) {
    override def defaultValue = true
    override def dbColumnName = "is_active"
  }
  object Description extends MappedText(this) {
    override def dbColumnName = "description"
  }
  object CreatedByUserId extends MappedString(this, 255) {
    override def dbColumnName = "created_by_user_id"
  }
  object UpdatedByUserId extends MappedString(this, 255) {
    override def dbColumnName = "updated_by_user_id"
  }

  override def abacRuleId: String = AbacRuleId.get
  override def ruleName: String = RuleName.get
  override def ruleCode: String = RuleCode.get
  override def isActive: Boolean = IsActive.get
  override def description: String = Description.get
  override def createdByUserId: String = CreatedByUserId.get
  override def updatedByUserId: String = UpdatedByUserId.get
}

object MappedAbacRule extends MappedAbacRule with LongKeyedMetaMapper[MappedAbacRule] {
  override def dbTableName = "abac_rule"
  override def dbIndexes: List[BaseIndex[MappedAbacRule]] = Index(AbacRuleId) :: Index(RuleName) :: Index(CreatedByUserId) :: super.dbIndexes
}

trait AbacRuleProvider {
  def getAbacRuleById(ruleId: String): Box[AbacRule]
  def getAbacRuleByName(ruleName: String): Box[AbacRule]
  def getAllAbacRules(): List[AbacRule]
  def getActiveAbacRules(): List[AbacRule]
  def createAbacRule(
    ruleName: String,
    ruleCode: String,
    description: String,
    isActive: Boolean,
    createdBy: String
  ): Box[AbacRule]
  def updateAbacRule(
    ruleId: String,
    ruleName: String,
    ruleCode: String,
    description: String,
    isActive: Boolean,
    updatedBy: String
  ): Box[AbacRule]
  def deleteAbacRule(ruleId: String): Box[Boolean]
}

object MappedAbacRuleProvider extends AbacRuleProvider {
  
  override def getAbacRuleById(ruleId: String): Box[AbacRule] = {
    MappedAbacRule.find(By(MappedAbacRule.AbacRuleId, ruleId))
  }

  override def getAbacRuleByName(ruleName: String): Box[AbacRule] = {
    MappedAbacRule.find(By(MappedAbacRule.RuleName, ruleName))
  }

  override def getAllAbacRules(): List[AbacRule] = {
    MappedAbacRule.findAll()
  }

  override def getActiveAbacRules(): List[AbacRule] = {
    MappedAbacRule.findAll(By(MappedAbacRule.IsActive, true))
  }

  override def createAbacRule(
    ruleName: String,
    ruleCode: String,
    description: String,
    isActive: Boolean,
    createdBy: String
  ): Box[AbacRule] = {
    tryo {
      MappedAbacRule.create
        .RuleName(ruleName)
        .RuleCode(ruleCode)
        .Description(description)
        .IsActive(isActive)
        .CreatedByUserId(createdBy)
        .UpdatedByUserId(createdBy)
        .saveMe()
    }
  }

  override def updateAbacRule(
    ruleId: String,
    ruleName: String,
    ruleCode: String,
    description: String,
    isActive: Boolean,
    updatedBy: String
  ): Box[AbacRule] = {
    for {
      rule <- MappedAbacRule.find(By(MappedAbacRule.AbacRuleId, ruleId))
      updatedRule <- tryo {
        rule
          .RuleName(ruleName)
          .RuleCode(ruleCode)
          .Description(description)
          .IsActive(isActive)
          .UpdatedByUserId(updatedBy)
          .saveMe()
      }
    } yield updatedRule
  }

  override def deleteAbacRule(ruleId: String): Box[Boolean] = {
    for {
      rule <- MappedAbacRule.find(By(MappedAbacRule.AbacRuleId, ruleId))
      deleted <- tryo(rule.delete_!)
    } yield deleted
  }
}