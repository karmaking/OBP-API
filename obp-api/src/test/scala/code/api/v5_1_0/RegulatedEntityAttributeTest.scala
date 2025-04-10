package code.api.v5_1_0

import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON._
import code.api.util.APIUtil.OAuth._
import code.api.util.ApiRole._
import code.api.util.ErrorMessages
import code.api.v5_1_0.APIMethods510.Implementations5_1_0
import code.entitlement.Entitlement
import code.setup.DefaultUsers
import com.github.dwickern.macros.NameOf.nameOf
import com.openbankproject.commons.model.ErrorMessage
import com.openbankproject.commons.util.ApiVersion
import net.liftweb.json.Serialization.write
import org.scalatest.Tag

class RegulatedEntityAttributeTest extends V510ServerSetup with DefaultUsers {

  override def beforeAll() {
    super.beforeAll()
  }

  override def afterAll() {
    super.afterAll()
  }
  
  object VersionOfApi extends Tag(ApiVersion.v5_1_0.toString)
  object Create extends Tag(nameOf(Implementations5_1_0.createRegulatedEntityAttribute))
  object Update extends Tag(nameOf(Implementations5_1_0.updateRegulatedEntityAttribute))
  object Delete extends Tag(nameOf(Implementations5_1_0.deleteRegulatedEntityAttribute))
  object GetAll extends Tag(nameOf(Implementations5_1_0.getAllRegulatedEntityAttributes))
  object GetOne extends Tag(nameOf(Implementations5_1_0.getRegulatedEntityAttributeById))

  lazy val entityId = "regulated-entity-id"

  feature(s"$Create - createRegulatedEntityAttribute") {
    scenario("Anonymous user fails", Create, VersionOfApi) {
      val request = (v5_1_0_Request / "regulated-entities" / entityId / "attributes").POST
      val response = makePostRequest(request, write(regulatedEntityAttributeRequestJsonV510))
      response.code should equal(401)
      response.body.extract[ErrorMessage].message should equal(ErrorMessages.UserNotLoggedIn)
    }

    scenario("User without role fails", Create, VersionOfApi) {
      val request = (v5_1_0_Request / "regulated-entities" / entityId / "attributes").POST <@ user1
      val response = makePostRequest(request, write(regulatedEntityAttributeRequestJsonV510))
      response.code should equal(403)
      response.body.extract[ErrorMessage].message should startWith(ErrorMessages.UserHasMissingRoles + CanCreateRegulatedEntityAttribute)
    }

    scenario("User with role succeeds", Create, VersionOfApi) {
      val entitlement = Entitlement.entitlement.vend.addEntitlement("", resourceUser1.userId, CanCreateRegulatedEntityAttribute.toString)
      val request = (v5_1_0_Request / "regulated-entities" / entityId / "attributes").POST <@ user1
      val response = makePostRequest(request, write(regulatedEntityAttributeRequestJsonV510))
      response.code should equal(201)
      Entitlement.entitlement.vend.deleteEntitlement(entitlement)
    }
  }

  feature(s"$Update - updateRegulatedEntityAttribute") {
    scenario("Unauthorized user cannot update", Update, VersionOfApi) {
      val request = (v5_1_0_Request / "regulated-entities" / entityId / "attributes" / "ATTRIBUTE_ID").PUT
      val response = makePutRequest(request, write(regulatedEntityAttributeRequestJsonV510))
      response.code should equal(401)
    }
  }

  feature(s"$Delete - deleteRegulatedEntityAttribute") {
    scenario("Unauthorized user cannot delete", Delete, VersionOfApi) {
      val request = (v5_1_0_Request / "regulated-entities" / entityId / "attributes" / "ATTRIBUTE_ID").DELETE
      val response = makeDeleteRequest(request)
      response.code should equal(401)
    }
  }

  feature(s"$GetAll - getAllRegulatedEntityAttributes") {
    scenario("Unauthorized user cannot view attributes", GetAll, VersionOfApi) {
      val request = (v5_1_0_Request / "regulated-entities" / entityId / "attributes").GET
      val response = makeGetRequest(request)
      response.code should equal(401)
    }
  }

  feature(s"$GetOne - getRegulatedEntityAttributeById") {
    scenario("Unauthorized user cannot view attribute by id", GetOne, VersionOfApi) {
      val request = (v5_1_0_Request / "regulated-entities" / entityId / "attributes" / "ATTRIBUTE_ID").GET
      val response = makeGetRequest(request)
      response.code should equal(401)
    }
  }
}
