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

  object mAbacRuleId extends MappedString(this, 255) {
    override def defaultValue = APIUtil.generateUUID()
  }
  object mRuleName extends MappedString(this, 255)
  object mRuleCode extends MappedText(this)
  object mIsActive extends MappedBoolean(this) {
    override def defaultValue = true
  }
  object mDescription extends MappedText(this)
  object mCreatedByUserId extends MappedString(this, 255)
  object mUpdatedByUserId extends MappedString(this, 255)

  override def abacRuleId: String = mAbacRuleId.get
  override def ruleName: String = mRuleName.get
  override def ruleCode: String = mRuleCode.get
  override def isActive: Boolean = mIsActive.get
  override def description: String = mDescription.get
  override def createdByUserId: String = mCreatedByUserId.get
  override def updatedByUserId: String = mUpdatedByUserId.get
}

object MappedAbacRule extends MappedAbacRule with LongKeyedMetaMapper[MappedAbacRule] {
  override def dbIndexes: List[BaseIndex[MappedAbacRule]] = Index(mAbacRuleId) :: Index(mRuleName) :: Index(mCreatedByUserId) :: super.dbIndexes
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
    MappedAbacRule.find(By(MappedAbacRule.mAbacRuleId, ruleId))
  }

  override def getAbacRuleByName(ruleName: String): Box[AbacRule] = {
    MappedAbacRule.find(By(MappedAbacRule.mRuleName, ruleName))
  }

  override def getAllAbacRules(): List[AbacRule] = {
    MappedAbacRule.findAll()
  }

  override def getActiveAbacRules(): List[AbacRule] = {
    MappedAbacRule.findAll(By(MappedAbacRule.mIsActive, true))
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
        .mRuleName(ruleName)
        .mRuleCode(ruleCode)
        .mDescription(description)
        .mIsActive(isActive)
        .mCreatedByUserId(createdBy)
        .mUpdatedByUserId(createdBy)
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
      rule <- MappedAbacRule.find(By(MappedAbacRule.mAbacRuleId, ruleId))
      updatedRule <- tryo {
        rule
          .mRuleName(ruleName)
          .mRuleCode(ruleCode)
          .mDescription(description)
          .mIsActive(isActive)
          .mUpdatedByUserId(updatedBy)
          .saveMe()
      }
    } yield updatedRule
  }

  override def deleteAbacRule(ruleId: String): Box[Boolean] = {
    for {
      rule <- MappedAbacRule.find(By(MappedAbacRule.mAbacRuleId, ruleId))
      deleted <- tryo(rule.delete_!)
    } yield deleted
  }
}