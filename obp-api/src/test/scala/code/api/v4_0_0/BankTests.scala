package code.api.v4_0_0

import code.api.ErrorMessage
import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON.bankJSONV220
import code.api.util.APIUtil.OAuth._
import code.api.util.ApiRole.CanCreateBank
import code.api.util.ErrorMessages.UserHasMissingRoles
import code.api.util.{ApiRole, ApiVersion, ErrorMessages, NewStyle}
import code.api.v4_0_0.APIMethods400.Implementations4_0_0
import code.entitlement.Entitlement
import code.setup.{APIResponse, DefaultUsers}
import com.github.dwickern.macros.NameOf.nameOf
import net.liftweb.json.Serialization.write
import org.scalatest.Tag

class BankTests extends V400ServerSetupAsync with DefaultUsers {

   override def beforeAll() {
     super.beforeAll()
   }

   override def afterAll() {
     super.afterAll()
   }

  /**
    * Test tags
    * Example: To run tests with tag "getPermissions":
    * 	mvn test -D tagsToInclude
    *
    *  This is made possible by the scalatest maven plugin
    */
  object VersionOfApi extends Tag(ApiVersion.v4_0_0.toString)
  object ApiEndpoint1 extends Tag(nameOf(Implementations4_0_0.createBank))

  feature("Assuring that endpoint createBank works as expected - v4.0.0") {

    scenario("We try to consume endpoint createBank - Anonymous access", ApiEndpoint1, VersionOfApi) {
      When("We make the request")
      val requestGet = (v4_0_0_Request / "banks").POST
      val responseGet = makePostRequestAsync(requestGet, write(bankJSONV220))
      Then("We should get a 400")
      And("We should get a message: " + ErrorMessages.UserNotLoggedIn)
      responseGet map { r =>
          r.code should equal(400)
          r.body.extract[ErrorMessage].message should equal(ErrorMessages.UserNotLoggedIn)
      }
    }

    scenario("We try to consume endpoint createBank without proper role - Authorized access", ApiEndpoint1, VersionOfApi) {
      When("We make the request")
      val requestGet = (v4_0_0_Request / "banks").POST <@ (user1)
      val responseGet = makePostRequestAsync(requestGet, write(bankJSONV220))
      Then("We should get a 403")
      And("We should get a message: " + s"$CanCreateBank entitlement required")
      responseGet map { r =>
          r.code should equal(403)
          r.body.extract[ErrorMessage].message should equal(UserHasMissingRoles + CanCreateBank)
      }
    }

    scenario("We try to consume endpoint createBank with proper role - Authorized access", ApiEndpoint1, VersionOfApi) {
      When("We add required entitlement")
      Entitlement.entitlement.vend.addEntitlement("", resourceUser1.userId, ApiRole.CanCreateBank.toString)
      And("We make the request")
      val requestGet = (v4_0_0_Request / "banks").POST <@ (user1)
      val response = for {
        before <- NewStyle.function.getEntitlementsByUserId(resourceUser1.userId, None) map {
          _.exists( e => e.roleName == ApiRole.CanCreateEntitlementAtOneBank.toString && e.bankId == bankJSONV220.id)
        }
        response: APIResponse <- makePostRequestAsync(requestGet, write(bankJSONV220))
        after <- NewStyle.function.getEntitlementsByUserId(resourceUser1.userId, None) map {
          _.exists( e => e.roleName == ApiRole.CanCreateEntitlementAtOneBank.toString && e.bankId == bankJSONV220.id)
        }
      } yield (before, after, response)
      Then("We should get a 201")
      response map { r =>
          r._1 should equal(false) // Before we create a bank there is no role CanCreateEntitlementAtOneBank
          r._2 should equal(true) // After we create a bank there is a role CanCreateEntitlementAtOneBank
          r._3.code should equal(201)
      }
    }
  }


 }