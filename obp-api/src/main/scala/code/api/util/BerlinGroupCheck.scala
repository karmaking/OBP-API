package code.api.util

import code.api.APIFailureNewStyle
import code.api.util.APIUtil.fullBoxOrException
import com.openbankproject.commons.model.User
import com.openbankproject.commons.util.ApiVersion
import net.liftweb.common.{Box, Empty}
import net.liftweb.http.provider.HTTPParam

object BerlinGroupCheck {

  // Parse mandatory headers from a comma-separated string
  private val berlinGroupMandatoryHeaders: List[String] = APIUtil.getPropsValue("berlin_group_mandatory_headers", defaultValue = "X-Request-ID,PSU-IP-Address,PSU-Device-ID,PSU-Device-Name")
    .split(",")
    .map(_.trim.toLowerCase)
    .toList.filterNot(_.isEmpty)
  private val berlinGroupMandatoryHeaderConsent = APIUtil.getPropsValue("berlin_group_mandatory_header_consent", defaultValue = "TPP-Redirect-URL")
    .split(",")
    .map(_.trim.toLowerCase)
    .toList.filterNot(_.isEmpty)

  private def validateHeaders(verb: String, url: String, reqHeaders: List[HTTPParam], forwardResult: (Box[User], Option[CallContext])): (Box[User], Option[CallContext]) = {
    val headerMap = reqHeaders.map(h => h.name.toLowerCase -> h).toMap
    val missingHeaders = if(url.contains(ApiVersion.berlinGroupV13.urlPrefix) && url.endsWith("/consent"))
      (berlinGroupMandatoryHeaders ++ berlinGroupMandatoryHeaderConsent).filterNot(headerMap.contains)
    else
      berlinGroupMandatoryHeaders.filterNot(headerMap.contains)

    if (missingHeaders.isEmpty) {
      forwardResult // All mandatory headers are present
    } else {
      (fullBoxOrException(Empty ~> APIFailureNewStyle(s"${ErrorMessages.MissingMandatoryBerlinGroupHeaders}(${missingHeaders.mkString(", ")})", 400, forwardResult._2.map(_.toLight))), forwardResult._2)
    }
  }

  def validate(body: Box[String], verb: String, url: String, reqHeaders: List[HTTPParam], forwardResult: (Box[User], Option[CallContext])): (Box[User], Option[CallContext]) = {
    if(url.contains(ApiVersion.berlinGroupV13.urlPrefix)) {
      validateHeaders(verb, url, reqHeaders, forwardResult) match {
        case (user, _) if user.isDefined || user == Empty => // All good. Chain another check
          // Verify signed request (Berlin Group)
          BerlinGroupSigning.verifySignedRequest(body, verb, url, reqHeaders, forwardResult)
        case forwardError => // Forward error case
          forwardError
      }
    } else {
      forwardResult
    }
  }

}
