package code.metadata.tags

import net.liftweb.util.SimpleInjector
import java.util.Date
import net.liftweb.common.Box
import code.model.Tag

object Tags  extends SimpleInjector {

  val tags = new Inject(buildOne _) {}
  
  def buildOne: Tags = MongoTransactionTags
  
}

trait Tags {
  
  def getTags(bankId : String, accountId : String, transactionId: String)() : List[Tag]
  def addTag(bankId : String, accountId : String, transactionId: String)(userId: String, viewId : Long, tagText : String, datePosted : Date) : Box[Tag]
  def deleteTag(bankId : String, accountId : String, transactionId: String)(tagId : String) : Box[Unit]
  
}