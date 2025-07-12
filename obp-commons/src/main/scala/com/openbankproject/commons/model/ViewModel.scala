/**
Open Bank Project - API
Copyright (C) 2011-2019, TESOBE GmbH.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE GmbH.
Osloer Strasse 16/17
Berlin 13359, Germany

This product includes software developed at
TESOBE (http://www.tesobe.com/)

  */


package com.openbankproject.commons.model

class AliasType
class Alias extends AliasType
object PublicAlias extends Alias
object PrivateAlias extends Alias
object NoAlias extends AliasType
case class AccountName(display: String, aliasType: AliasType)
case class Permission(
                       user : User,
                       views : List[View]
                     )


/*
View Specification
Defines how the View should be named, i.e. if it is public, the Alias behaviour, what fields can be seen and what actions can be done through it.
 */
trait ViewSpecification {
  def description: String
  def metadata_view: String
  def is_public: Boolean
  def is_firehose: Option[Boolean] = None
  def which_alias_to_use: String
  def hide_metadata_if_alias_used: Boolean
  def allowed_actions : List[String]
  def can_grant_access_to_views : Option[List[String]] = None
  def can_revoke_access_to_views : Option[List[String]] = None
}

/*
The JSON that should be supplied to create a custom view. Conforms to ViewSpecification
 */
case class CreateViewJson(
                           name: String,
                           description: String,
                           metadata_view: String,
                           is_public: Boolean,
                           which_alias_to_use: String,
                           hide_metadata_if_alias_used: Boolean,
                           allowed_actions : List[String],
                           override val can_grant_access_to_views : Option[List[String]] = None,
                           override val can_revoke_access_to_views : Option[List[String]] = None
                         ) extends ViewSpecification


/*
The JSON that should be supplied to update a system view. Conforms to ViewSpecification
 */
case class UpdateViewJSON(
                           description: String,
                           metadata_view: String,
                           is_public: Boolean,
                           override val is_firehose: Option[Boolean] = None,
                           which_alias_to_use: String,
                           hide_metadata_if_alias_used: Boolean,
                           allowed_actions: List[String],
                           override val can_grant_access_to_views : Option[List[String]] = None,
                           override val can_revoke_access_to_views : Option[List[String]] = None) extends ViewSpecification


trait View {
  def id: Long

  // metedataView is tricky, it used for all the transaction meta in different views share the same metadataView.
  // we create, get, update transaction.meta call the deufault metadataView. Not the currentView.
  // eg: If current view is _tesobe, you set metadataView is `owner`, then all the transaction.meta will just point to `owner` view.
  // Look into the following method in code, you will know more about it:
  //  code.metadata.wheretags.MapperWhereTags.addWhereTag
  //  val metadateViewId = Views.views.vend.getMetadataViewId(BankIdAccountId(bankId, accountId), viewId)
  def metadataView: String

  //This is used for distinguishing all the views
  //For now, we need have some system views and user created views.
  // 1 `System Views` : eg: owner, accountant ... They are the fixed views, developers can not modify it.
  // 2 `User Created Views`: Start with _, eg _son, _wife ... The developers can update the fields for these views.
  def isSystem: Boolean

  def isFirehose: Boolean

  def isPublic: Boolean

  def isPrivate: Boolean

  //these three Ids are used together to uniquely identify a view:
  // eg: a view = viewId(`owner`) + accountId('e4f001fe-0f0d-4f93-a8b2-d865077315ec')+bankId('gh.29.uk')
  // the viewId is not OBP uuid here, view.viewId is view.name without spaces and lowerCase.  (view.name = my life) <---> (view-permalink = mylife)
  // aslo @code.views.MapperViews.createView see how we create the viewId.
  def viewId: ViewId

  def accountId: AccountId

  def bankId: BankId

  //and here is the unique identifier
  def uid: BankIdAccountIdViewId = BankIdAccountIdViewId(bankId, accountId, viewId)

  //The name is the orignal value from developer, when they create the views.
  // It can be any string value, also see the viewId,
  // the viewId is not OBP uuid here, view.viewId is view.name without spaces and lowerCase.  (view.name = my life) <---> (view-permalink = mylife)
  // aslo @code.views.MapperViews.createView see how we create the viewId.
  def name: String

  //the Value from developer, can be any string value.
  def description: String

  /** These users are tricky, this use ManyToMany relationship,
    * 1st: when create view, we need carefully map this view to the owner user.
    * 2nd: the view can grant the access to any other (not owner) users. eg: Simon's accountant view can grant access to Carola, then Carola can see Simon's accountant data
    * also look into some createView methods in code, you can understand more:
    * create1: code.bankconnectors.Connector.createViews
    * after createViews method, always need call addPermission(v.uid, user). This will create this field
    * Create2: code.model.dataAccess.BankAccountCreation.createOwnerView
    * after create view, always need call `addPermission(ownerViewUID, user)`, this will create this field
    * create3: code.model.dataAccess.AuthUser#updateUserAccountViews
    * after create view, always need call `getOrCreateViewPrivilege(view,user)`, this will create this filed
    * Both uses should be in this List.
    */
  def users: List[User]

  //the view settings
  def usePublicAliasIfOneExists: Boolean

  def usePrivateAliasIfOneExists: Boolean

  def hideOtherAccountMetadataIfAlias: Boolean
  /**
   * These three will get the allowed actions from viewPermission table
   */
  def allowed_actions : List[String]
  def canGrantAccessToViews : Option[List[String]] = None
  def canRevokeAccessToViews : Option[List[String]] = None
  
  def createViewAndPermissions(viewSpecification : ViewSpecification) : Unit
  def deleteViewPermissions :List[Boolean]
}