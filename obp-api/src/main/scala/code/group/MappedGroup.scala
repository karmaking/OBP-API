package code.group

import code.util.MappedUUID
import net.liftweb.common.{Box, Empty, Full}
import net.liftweb.mapper._
import net.liftweb.util.Helpers.tryo

import scala.concurrent.Future
import com.openbankproject.commons.ExecutionContext.Implicits.global

object MappedGroupProvider extends GroupProvider {
  
  override def createGroup(
    bankId: Option[String],
    groupName: String,
    groupDescription: String,
    listOfRoles: List[String],
    isEnabled: Boolean
  ): Box[Group] = {
    tryo {
      MappedGroup.create
        .BankId(bankId.getOrElse(""))
        .GroupName(groupName)
        .GroupDescription(groupDescription)
        .ListOfRoles(listOfRoles.mkString(","))
        .IsEnabled(isEnabled)
        .saveMe()
    }
  }
  
  override def getGroup(groupId: String): Box[Group] = {
    MappedGroup.find(By(MappedGroup.GroupId, groupId))
  }
  
  override def getGroupsByBankId(bankId: Option[String]): Future[Box[List[Group]]] = {
    Future {
      tryo {
        bankId match {
          case Some(id) => 
            MappedGroup.findAll(By(MappedGroup.BankId, id))
          case None => 
            MappedGroup.findAll(By(MappedGroup.BankId, ""))
        }
      }
    }
  }
  
  override def getAllGroups(): Future[Box[List[Group]]] = {
    Future {
      tryo {
        MappedGroup.findAll()
      }
    }
  }
  
  override def updateGroup(
    groupId: String,
    groupName: Option[String],
    groupDescription: Option[String],
    listOfRoles: Option[List[String]],
    isEnabled: Option[Boolean]
  ): Box[Group] = {
    MappedGroup.find(By(MappedGroup.GroupId, groupId)).flatMap { group =>
      tryo {
        groupName.foreach(name => group.GroupName(name))
        groupDescription.foreach(desc => group.GroupDescription(desc))
        listOfRoles.foreach(roles => group.ListOfRoles(roles.mkString(",")))
        isEnabled.foreach(enabled => group.IsEnabled(enabled))
        group.saveMe()
      }
    }
  }
  
  override def deleteGroup(groupId: String): Box[Boolean] = {
    MappedGroup.find(By(MappedGroup.GroupId, groupId)).flatMap { group =>
      tryo {
        group.delete_!
      }
    }
  }
}

class MappedGroup extends Group with LongKeyedMapper[MappedGroup] with IdPK with CreatedUpdated {
  
  def getSingleton = MappedGroup
  
  object GroupId extends MappedUUID(this)
  object BankId extends MappedString(this, 255) // Empty string for system-level groups
  object GroupName extends MappedString(this, 255)
  object GroupDescription extends MappedText(this)
  object ListOfRoles extends MappedText(this) // Comma-separated list of roles
  object IsEnabled extends MappedBoolean(this)
  
  override def groupId: String = GroupId.get.toString
  override def bankId: Option[String] = {
    val id = BankId.get
    if (id == null || id.isEmpty) None else Some(id)
  }
  override def groupName: String = GroupName.get
  override def groupDescription: String = GroupDescription.get
  override def listOfRoles: List[String] = {
    val rolesStr = ListOfRoles.get
    if (rolesStr == null || rolesStr.isEmpty) List.empty
    else rolesStr.split(",").map(_.trim).filter(_.nonEmpty).toList
  }
  override def isEnabled: Boolean = IsEnabled.get
}

object MappedGroup extends MappedGroup with LongKeyedMetaMapper[MappedGroup] {
  override def dbTableName = "Group" // define the DB table name
  override def dbIndexes = Index(GroupId) :: Index(BankId) :: super.dbIndexes
}