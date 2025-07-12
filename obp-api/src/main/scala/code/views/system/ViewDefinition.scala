package code.views.system

import code.api.Constant._
import code.api.util.APIUtil.{isValidCustomViewId, isValidSystemViewId}
import code.api.util.ErrorMessages.{CreateSystemViewError, InvalidCustomViewFormat, InvalidSystemViewFormat}
import code.util.{AccountIdString, UUIDString}
import com.openbankproject.commons.model._
import net.liftweb.common.{Box, Full}
import net.liftweb.common.Box.tryo
import net.liftweb.mapper._
import code.api.Constant._

class ViewDefinition extends View with LongKeyedMapper[ViewDefinition] with ManyToMany with CreatedUpdated{
  def getSingleton = ViewDefinition

  def primaryKeyField = id_

  object id_ extends MappedLongIndex(this)
  object name_ extends MappedString(this, 125)
  object description_ extends MappedString(this, 255)
  object bank_id extends UUIDString(this) {
    override def defaultValue: Null = null
  }
  object account_id extends AccountIdString(this) {
    override def defaultValue: Null = null
  }
  object view_id extends UUIDString(this)
  
  @deprecated("This field is not used in api code anymore","13-12-2019")
  object composite_unique_key extends MappedString(this, 512)
  object metadataView_ extends UUIDString(this)
  object isSystem_ extends MappedBoolean(this){
    override def defaultValue = false
    override def dbIndexed_? = true
  }
  object isPublic_ extends MappedBoolean(this){
    override def defaultValue = false
    override def dbIndexed_? = true
  }
  object isFirehose_ extends MappedBoolean(this){
    override def defaultValue = true
    override def dbIndexed_? = true
  }
  object usePrivateAliasIfOneExists_ extends MappedBoolean(this){
    override def defaultValue = false
  }
  object usePublicAliasIfOneExists_ extends MappedBoolean(this){
    override def defaultValue = false
  }
  object hideOtherAccountMetadataIfAlias_ extends MappedBoolean(this){
    override def defaultValue = false
  }

  def createViewAndPermissions(viewSpecification : ViewSpecification) = {
    if(viewSpecification.which_alias_to_use == "public"){
      usePublicAliasIfOneExists_(true)
      usePrivateAliasIfOneExists_(false)
    } else if(viewSpecification.which_alias_to_use == "private"){
      usePublicAliasIfOneExists_(false)
      usePrivateAliasIfOneExists_(true)
    } else {
      usePublicAliasIfOneExists_(false)
      usePrivateAliasIfOneExists_(false)
    }

    hideOtherAccountMetadataIfAlias_(viewSpecification.hide_metadata_if_alias_used)
    description_(viewSpecification.description)
    isPublic_(viewSpecification.is_public)
    isFirehose_(viewSpecification.is_firehose.getOrElse(false))
    metadataView_(viewSpecification.metadata_view)
    
    ViewPermission.createViewPermissions(
      this,
      viewSpecification.allowed_actions,
      viewSpecification.can_grant_access_to_views.getOrElse(Nil),
      viewSpecification.can_revoke_access_to_views.getOrElse(Nil)
    )
    
  }

  

  def id: Long = id_.get
  def viewId : ViewId = ViewId(view_id.get)
  
  @deprecated("This field is not used in api code anymore","13-12-2019")
  def viewIdInternal: String = composite_unique_key.get
  //if metadataView_ = null or empty, we need use the current view's viewId.
  def metadataView = if (metadataView_.get ==null || metadataView_.get == "") view_id.get else metadataView_.get
  def users : List[User] = Nil
  def bankId = BankId(bank_id.get)
  def accountId = AccountId(account_id.get)
  def name: String = name_.get
  def description : String = description_.get
  def isPublic : Boolean = isPublic_.get
  def isPrivate : Boolean = !isPublic_.get
  def isFirehose : Boolean = isFirehose_.get
  def isSystem: Boolean = isSystem_.get
  //the view settings
  def usePrivateAliasIfOneExists: Boolean = usePrivateAliasIfOneExists_.get
  def usePublicAliasIfOneExists: Boolean = usePublicAliasIfOneExists_.get
  def hideOtherAccountMetadataIfAlias: Boolean = hideOtherAccountMetadataIfAlias_.get

  override def allowed_actions : List[String] = ViewPermission.findViewPermissions(this).map(_.permission.get).distinct

  override def canGrantAccessToViews : Option[List[String]] = {
   ViewPermission.findViewPermission(this, CAN_GRANT_ACCESS_TO_VIEWS).flatMap(vp => 
    {
      vp.extraData.get match {
        case value if(value != null && !value.isEmpty) => Some(value.split(",").toList.map(_.trim))
        case _ => None
      }
    })
  }
  
  override def canRevokeAccessToViews : Option[List[String]] = {
    ViewPermission.findViewPermission(this, CAN_REVOKE_ACCESS_TO_VIEWS).flatMap(vp =>
    {
      vp.extraData.get match {
        case value if(value != null && !value.isEmpty) => Some(value.split(",").toList.map(_.trim))
        case _ => None
      }
    })
  }
}

object ViewDefinition extends ViewDefinition with LongKeyedMetaMapper[ViewDefinition] {
  override def dbIndexes: List[BaseIndex[ViewDefinition]] = UniqueIndex(composite_unique_key) :: super.dbIndexes
  override def beforeSave = List(
    t =>{
      tryo {
        val compositeUniqueKey = getUniqueKey(t.bank_id.get, t.account_id.get, t.view_id.get)
        t.composite_unique_key(compositeUniqueKey)
      }

      if (t.isSystem && !isValidSystemViewId(t.view_id.get)) {
        throw new RuntimeException(InvalidSystemViewFormat+s"Current view_id (${t.view_id.get})")
      }
      if (!t.isSystem && !isValidCustomViewId(t.view_id.get)) {
        throw new RuntimeException(InvalidCustomViewFormat+s"Current view_id (${t.view_id.get})")
      }
      
      //sanity checks
      if (!t.isSystem && (t.bank_id ==null || t.account_id == null)) {
        throw new RuntimeException(CreateSystemViewError+s"Current view.isSystem${t.isSystem}, bank_id${t.bank_id}, account_id${t.account_id}")
      }
    }
  )

  def findSystemView(viewId: String): Box[ViewDefinition] = {
    ViewDefinition.find(
      NullRef(ViewDefinition.bank_id),
      NullRef(ViewDefinition.account_id),
      By(ViewDefinition.isSystem_, true),
      By(ViewDefinition.view_id, viewId),
    )
  }
  def getSystemViews(): List[ViewDefinition] = {
    ViewDefinition.findAll(
      By(ViewDefinition.isSystem_, true)
    )
  }

  def findCustomView(bankId: String, accountId: String, viewId: String): Box[ViewDefinition] = {
    ViewDefinition.find(
      By(ViewDefinition.bank_id, bankId),
      By(ViewDefinition.account_id, accountId),
      By(ViewDefinition.isSystem_, false),
      By(ViewDefinition.view_id, viewId),
    )
  }
  def getCustomViews(): List[ViewDefinition] = {
    ViewDefinition.findAll(
      By(ViewDefinition.isSystem_, false)
    )
  }
  
  @deprecated("This is method only used for migration stuff, please use @findCustomView and @findSystemView instead.","13-12-2019")
  def findByUniqueKey(bankId: String, accountId: String, viewId: String): Box[ViewDefinition] = {
    val uniqueKey = getUniqueKey(bankId, accountId, viewId)
    ViewDefinition.find(
      By(ViewDefinition.composite_unique_key, uniqueKey)
    )
  }

  def accountFilter(bankId : BankId, accountId : AccountId) : List[QueryParam[ViewDefinition]] = {
    By(bank_id, bankId.value) :: By(account_id, accountId.value) :: Nil
  }
  
  @deprecated("This is method only used for migration stuff, do not use api code.","13-12-2019")
  def getUniqueKey(bankId: String, accountId: String, viewId: String) = List(bankId, accountId, viewId).mkString("|","|--|","|")
}