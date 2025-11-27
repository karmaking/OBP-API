package code.group

import net.liftweb.common.Box
import net.liftweb.util.SimpleInjector

import scala.concurrent.Future

object Group extends SimpleInjector {
  val group = new Inject(buildOne _) {}
  
  def buildOne: GroupProvider = MappedGroupProvider
}

trait GroupProvider {
  def createGroup(
    bankId: Option[String],
    groupName: String,
    groupDescription: String,
    listOfRoles: List[String],
    isEnabled: Boolean
  ): Box[Group]
  
  def getGroup(groupId: String): Box[Group]
  def getGroupsByBankId(bankId: Option[String]): Future[Box[List[Group]]]
  def getAllGroups(): Future[Box[List[Group]]]
  
  def updateGroup(
    groupId: String,
    groupName: Option[String],
    groupDescription: Option[String],
    listOfRoles: Option[List[String]],
    isEnabled: Option[Boolean]
  ): Box[Group]
  
  def deleteGroup(groupId: String): Box[Boolean]
}

trait Group {
  def groupId: String
  def bankId: Option[String]
  def groupName: String
  def groupDescription: String
  def listOfRoles: List[String]
  def isEnabled: Boolean
}