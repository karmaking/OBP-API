package code.views.system

import code.api.Constant.{CAN_GRANT_ACCESS_TO_VIEWS, CAN_REVOKE_ACCESS_TO_VIEWS}
import code.util.UUIDString
import com.openbankproject.commons.model._
import net.liftweb.common.Box
import net.liftweb.mapper._


class ViewPermission extends LongKeyedMapper[ViewPermission] with IdPK with CreatedUpdated {
  def getSingleton = ViewPermission
  object bank_id extends MappedString(this, 255)
  object account_id extends MappedString(this, 255)
  object view_id extends UUIDString(this)
  object permission extends MappedString(this, 255)
  
  //this is for special permissions like CAN_REVOKE_ACCESS_TO_VIEWS and CAN_GRANT_ACCESS_TO_VIEWS, it will be a list of view ids , 
  // eg: owner,auditor,accountant,firehose,standard,StageOne,ManageCustomViews,ReadAccountsBasic
  object extraData extends MappedString(this, 1024) 
}
object ViewPermission extends ViewPermission with LongKeyedMetaMapper[ViewPermission] {
  override def dbIndexes: List[BaseIndex[ViewPermission]] = UniqueIndex(bank_id, account_id, view_id, permission) :: super.dbIndexes
  
  def findCustomViewPermissions(bankId: BankId, accountId: AccountId, viewId: ViewId): List[ViewPermission] =
    ViewPermission.findAll(
      By(ViewPermission.bank_id, bankId.value),
      By(ViewPermission.account_id, accountId.value),
      By(ViewPermission.view_id, viewId.value)
    )  
    
  def findSystemViewPermissions(viewId: ViewId): List[ViewPermission] =
    ViewPermission.findAll(
      NullRef(ViewPermission.bank_id),
      NullRef(ViewPermission.account_id),
      By(ViewPermission.view_id, viewId.value)
    )
    
  def findCustomViewPermission(bankId: BankId, accountId: AccountId, viewId: ViewId, permission: String): Box[ViewPermission] =
    ViewPermission.find(
      By(ViewPermission.bank_id, bankId.value),
      By(ViewPermission.account_id, accountId.value),
      By(ViewPermission.view_id, viewId.value),
      By(ViewPermission.permission,permission)
    )  
    
  def findSystemViewPermission(viewId: ViewId, permission: String): Box[ViewPermission] =
    ViewPermission.find(
      NullRef(ViewPermission.bank_id),
      NullRef(ViewPermission.account_id),
      By(ViewPermission.view_id, viewId.value),
      By(ViewPermission.permission,permission),
    )

  /**
   * Finds the permissions for a given view, if it is sytem view, 
   * it will search in system view permission, otherwise it will search in custom view permissions.
   * @param view
   * @return
   */
  def findViewPermissions(view: View): List[ViewPermission] =
    if(view.isSystem) {
      findSystemViewPermissions(view.viewId)
    } else {
      findCustomViewPermissions(view.bankId, view.accountId, view.viewId)
    }
    
  def findViewPermission(view: View, permission: String): Box[ViewPermission] =
    if(view.isSystem) {
      findSystemViewPermission(view.viewId, permission)
    } else {
      findCustomViewPermission(view.bankId, view.accountId, view.viewId, permission)
    }

  /**
   * This method will first remove all the current permissons.
   * and will create new ones accouding to the parameters.
   * 
   * This is the logic from ViewDefinition before. because we can only update all the permissions before,
   * we may support only update one permissioin later.
   */
  def createViewPermissions(
    viewDefinition: View,
    permissionNames: List[String],
    canGrantAccessToViews: List[String] = Nil,
    canRevokeAccessToViews: List[String] = Nil
  ): Unit = {

    // Delete all existing permissions for the view
    viewDefinition.deleteViewPermissions

    // Determine bank_id and account_id for system or custom views
    val (bankId, accountId) =
      if (viewDefinition.isSystem)
        (null, null)
      else
        (viewDefinition.bankId.value, viewDefinition.accountId.value)

    // Create fresh permission entries
    permissionNames.foreach { permissionName =>
      val extraData = permissionName match {
        case CAN_GRANT_ACCESS_TO_VIEWS  => canGrantAccessToViews.mkString(",")
        case CAN_REVOKE_ACCESS_TO_VIEWS => canRevokeAccessToViews.mkString(",")
        case _                          => null
      }

      ViewPermission.create
        .bank_id(bankId)
        .account_id(accountId)
        .view_id(viewDefinition.viewId.value)
        .permission(permissionName)
        .extraData(extraData)
        .save
    }
  }

}
