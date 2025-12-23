package code.api.v2_2_0

import com.openbankproject.commons.model.ErrorMessage
import scala.language.reflectiveCalls
import code.api.util.APIUtil.OAuth._
import scala.language.reflectiveCalls
import code.api.util.ApiRole
import scala.language.reflectiveCalls
import code.api.util.ErrorMessages.InvalidISOCurrencyCode
import scala.language.reflectiveCalls
import code.consumer.Consumers
import scala.language.reflectiveCalls
import code.scope.Scope
import scala.language.reflectiveCalls
import code.setup.DefaultUsers
import scala.language.reflectiveCalls
import com.github.dwickern.macros.NameOf.nameOf
import scala.language.reflectiveCalls
import com.openbankproject.commons.util.ApiVersion
import scala.language.reflectiveCalls
import org.scalatest.Tag
import scala.language.reflectiveCalls

class ExchangeRateTest extends V220ServerSetup with DefaultUsers {

  /**
    * Test tags
    * Example: To run tests with tag "getPermissions":
    * 	mvn test -D tagsToInclude
    *
    *  This is made possible by the scalatest maven plugin
    */
  object VersionOfApi extends Tag(ApiVersion.v2_2_0.toString)
  object ApiEndpoint1 extends Tag(nameOf(OBPAPI2_2_0.Implementations2_2_0.getCurrentFxRate))

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }
  
  feature("Assuring that Get Current FxRate works as expected - v2.2.0") {

    scenario("We Get Current FxRate", VersionOfApi, ApiEndpoint1) {
      val testBank = testBankId1
      val consumerId = Consumers.consumers.vend.getConsumerByConsumerKey(user1.get._1.key).map(_.id.get.toString).getOrElse("")
      Scope.scope.vend.addScope(testBank.value, consumerId, ApiRole.canReadFx.toString())
      val requestGet = (v2_2Request / "banks" / testBank.value / "fx" / "EUR" / "EUR" ).GET <@ (user1)
      val responseGet = makeGetRequest(requestGet)
      And("We should get a 200")
      responseGet.code should equal(200)
    }
    
    scenario("We Get Current FxRate with wrong ISO from currency code", VersionOfApi, ApiEndpoint1) {
      val testBank = testBankId1
      val consumerId = Consumers.consumers.vend.getConsumerByConsumerKey(user1.get._1.key).map(_.id.get.toString).getOrElse("")
      Scope.scope.vend.addScope(testBank.value, consumerId, ApiRole.canReadFx.toString())
      val requestGet = (v2_2Request / "banks" / testBank.value / "fx" / "EUR1" / "EUR" ).GET <@ (user1)
      val responseGet = makeGetRequest(requestGet)
      And("We should get a 400")
      responseGet.code should equal(400)
      responseGet.body.extract[ErrorMessage].message should startWith (InvalidISOCurrencyCode)
    }

    scenario("We Get Current FxRate with wrong ISO to currency code", VersionOfApi, ApiEndpoint1) {
      val testBank = testBankId1
      val consumerId = Consumers.consumers.vend.getConsumerByConsumerKey(user1.get._1.key).map(_.id.get.toString).getOrElse("")
      Scope.scope.vend.addScope(testBank.value, consumerId, ApiRole.canReadFx.toString())
      val requestGet = (v2_2Request / "banks" / testBank.value / "fx" / "EUR" / "EUR1" ).GET <@ (user1)
      val responseGet = makeGetRequest(requestGet)
      And("We should get a 400")
      responseGet.code should equal(400)
      responseGet.body.extract[ErrorMessage].message should startWith (InvalidISOCurrencyCode)
    }
    
  }

}
