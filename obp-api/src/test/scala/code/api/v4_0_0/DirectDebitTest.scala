package code.api.v4_0_0

import code.api.ErrorMessage
import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON
import code.api.util.APIUtil.OAuth._
import code.api.util.ApiRole.CanCreateDirectDebitAtOneBank
import code.api.util.ApiVersion
import code.api.util.ErrorMessages.{NoViewPermission, UserHasMissingRoles, UserNotLoggedIn}
import code.api.v4_0_0.OBPAPI4_0_0.Implementations4_0_0
import com.github.dwickern.macros.NameOf.nameOf
import net.liftweb.json.Serialization.write
import org.scalatest.Tag

class DirectDebitTest extends V400ServerSetup {
  /**
    * Test tags
    * Example: To run tests with tag "getPermissions":
    * 	mvn test -D tagsToInclude
    *
    *  This is made possible by the scalatest maven plugin
    */
  object VersionOfApi extends Tag(ApiVersion.v4_0_0.toString)
  object ApiEndpoint1 extends Tag(nameOf(Implementations4_0_0.createDirectDebit))
  object ApiEndpoint2 extends Tag(nameOf(Implementations4_0_0.createDirectDebitManagement))

  lazy val postDirectDebitJsonV400 = SwaggerDefinitionsJSON.postDirectDebitJsonV400
  lazy val bankId = randomBankId
  lazy val bankAccount = randomPrivateAccount(bankId)
  lazy val view = randomOwnerViewPermalink(bankId, bankAccount)

  feature(s"test $ApiEndpoint1 version $VersionOfApi - Unauthorized access") {
    scenario("We will call the endpoint without user credentials", ApiEndpoint1, VersionOfApi) {
      When("We make a request v4.0.0")
      val request400 = (v4_0_0_Request / "banks" / bankId / "accounts" / bankAccount.id / view / "direct-debit").POST
      val response400 = makePostRequest(request400, write(postDirectDebitJsonV400))
      Then("We should get a 400")
      response400.code should equal(400)
      response400.body.extract[ErrorMessage].message should equal(UserNotLoggedIn)
    }
  }
  feature(s"test $ApiEndpoint1 version $VersionOfApi - Authorized access") {
    scenario("We will call the endpoint without user credentials", ApiEndpoint1, VersionOfApi) {
      When("We make a request v4.0.0")
      val request400 = (v4_0_0_Request / "banks" / bankId / "accounts" / bankAccount.id / view / "direct-debit").POST <@(user1)
      val response400 = makePostRequest(request400, write(postDirectDebitJsonV400))
      Then("We should get a 400")
      response400.code should equal(400)
      response400.body.extract[ErrorMessage].message should startWith(NoViewPermission)
    }
  }
  
  
  feature(s"test $ApiEndpoint2 version $VersionOfApi - Unauthorized access") {
    scenario("We will call the endpoint without user credentials", ApiEndpoint2, VersionOfApi) {
      When("We make a request v4.0.0")
      val request400 = (v4_0_0_Request / "management" / "banks" / bankId / "accounts" / bankAccount.id / "direct-debit").POST
      val response400 = makePostRequest(request400, write(postDirectDebitJsonV400))
      Then("We should get a 400")
      response400.code should equal(400)
      response400.body.extract[ErrorMessage].message should equal(UserNotLoggedIn)
    }
  }
  feature(s"test $ApiEndpoint2 version $VersionOfApi - Authorized access") {
    scenario("We will call the endpoint without user credentials", ApiEndpoint2, VersionOfApi) {
      When("We make a request v4.0.0")
      val request400 = (v4_0_0_Request / "management" / "banks" / bankId / "accounts" / bankAccount.id / "direct-debit").POST <@(user1)
      val response400 = makePostRequest(request400, write(postDirectDebitJsonV400))
      Then("We should get a 403")
      response400.code should equal(403)
      response400.body.extract[ErrorMessage].message should equal(UserHasMissingRoles + CanCreateDirectDebitAtOneBank)
    }
  }
  
  
}
