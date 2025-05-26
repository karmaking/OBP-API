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
package code.snippet

import code.api.util.APIUtil.callEndpoint
import code.api.util.{CustomJsonFormats, ErrorMessages}
import code.api.v5_1_0.ConsentsInfoJsonV510
import code.api.v5_1_0.OBPAPI5_1_0.Implementations5_1_0
import code.consumer.Consumers
import code.model.dataAccess.AuthUser
import code.util.Helper.{MdcLoggable, ObpS}
import code.util.HydraUtil.integrateWithHydra
import net.liftweb.common.{Failure, Full}
import net.liftweb.http.{GetRequest, RequestVar, S, SHtml}
import net.liftweb.json
import net.liftweb.json.{Extraction, Formats, JNothing}
import net.liftweb.util.CssSel
import net.liftweb.util.Helpers._
import sh.ory.hydra.api.AdminApi
import sh.ory.hydra.model.{AcceptConsentRequest, RejectRequest}

import scala.jdk.CollectionConverters.seqAsJavaListConverter
import scala.xml.NodeSeq

class ConsentScreen extends MdcLoggable {

  private object skipConsentScreenVar extends RequestVar(false)
  private object consentChallengeVar extends RequestVar(ObpS.param("consent_challenge").getOrElse(""))
  private object csrfVar extends RequestVar(ObpS.param("_csrf").getOrElse(""))
  implicit val formats: Formats = CustomJsonFormats.formats
  
  def submitAllowAction: Unit = {
    integrateWithHydra match {
      case true if !consentChallengeVar.isEmpty =>
        val acceptConsentRequestBody = new AcceptConsentRequest
        val adminApi: AdminApi = new AdminApi
        acceptConsentRequestBody.setRemember(skipConsentScreenVar.is)
        acceptConsentRequestBody.setRememberFor(0L)
        acceptConsentRequestBody.setGrantScope(List("openid").asJava)
        val completedRequest = adminApi.acceptConsentRequest(consentChallengeVar.is, acceptConsentRequestBody)
        S.redirectTo(completedRequest.getRedirectTo)
      case false =>
        S.redirectTo("/") // Home page
    }
  }
  
  def submitDenyAction: Unit = {
    integrateWithHydra match {
      case true if !consentChallengeVar.isEmpty =>
        val rejectRequestBody = new RejectRequest
        rejectRequestBody.setError("access_denied")
        rejectRequestBody.setErrorDescription("The resource owner denied the request")
        val adminApi: AdminApi = new AdminApi
        val completedRequest = adminApi.rejectConsentRequest(consentChallengeVar.is, rejectRequestBody)
        S.redirectTo("/") // Home page
      case false =>
        S.redirectTo("/") // Home page
    }
  }
  
  def consentScreenForm = {
    val username = AuthUser.getCurrentUser.map(_.name).getOrElse("")
    val adminApi: AdminApi = new AdminApi
    val consumerKey = adminApi.getConsentRequest(consentChallengeVar.is).getClient.getClientId
    val consumerName = Consumers.consumers.vend.getConsumerByConsumerKey(consumerKey).map(_.name.get).getOrElse("Unknown")
    "#username *" #> username &
      "#consumer_description_1 *" #> consumerName &
    "form" #> {
      "#skip_consent_screen_checkbox" #> SHtml.checkbox(skipConsentScreenVar, skipConsentScreenVar(_)) &
        "#allow_access_to_consent" #> SHtml.submit(s"Allow access", () => submitAllowAction) &
        "#deny_access_to_consent" #> SHtml.submit(s"Deny access", () => submitDenyAction)
    }
  }

  /**
   * Renders  the consents page.
   */
  def getConsents: CssSel = {

    val pathOfEndpoint = List(
      "my",
      "consents"
    )

    val getMyConsentsResult = callEndpoint(Implementations5_1_0.getMyConsents, pathOfEndpoint, GetRequest)

    getMyConsentsResult match {
      case Left(error) => {
        S.error(error._1)
        ".consent-entry" #> NodeSeq.Empty
      }
      case Right(response) => {
        tryo {json.parse(response).extract[ConsentsInfoJsonV510]} match {
          case Full(consentsInfoJsonV510) =>
            val consents = consentsInfoJsonV510.consents
            ".consent-entry" #> consents.map { consent =>
              ".consent-id *" #> consent.consent_id &
                ".consumer-id *" #> consent.consumer_id &
                ".jwt-payload *" #> json.prettyRender(consent.jwt_payload.map(Extraction.decompose).openOr(JNothing)) &
                ".status *" #> consent.status &
                ".api-standard *" #> consent.api_standard &
                ".revoke-form [action]" #> s"/my/consents/consent_id/${consent.consent_id}" &
                ".consent-id-input [value]" #> consent.consent_id
            }
          case Failure(msg, t, c) =>
            S.error(s"${ErrorMessages.UnknownError} $msg")
            ".consent-entry" #> NodeSeq.Empty
          case _ =>
            S.error(s"${ErrorMessages.UnknownError} Failed to parse response")
            ".consent-entry" #> NodeSeq.Empty
        }
      }
    }
  }
}
