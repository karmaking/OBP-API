package code.api.v7_0_0

import code.api.Constant
import code.api.util.APIUtil
import code.api.util.ErrorMessages.MandatoryPropertyIsNotSet
import code.api.v4_0_0.{EnergySource400, HostedAt400, HostedBy400}
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.util.ApiVersion
import net.liftweb.util.Props

object JSONFactory700 extends MdcLoggable {

  // Get git commit from build info
  lazy val gitCommit: String = {
    val commit = try {
      Props.get("git.commit.id", "unknown")
    } catch {
      case _: Throwable => "unknown"
    }
    commit
  }

  case class APIInfoJsonV700(
    version: String,
    version_status: String,
    git_commit: String,
    stage: String,
    connector: String,
    hostname: String,
    local_identity_provider: String,
    hosted_by: HostedBy400,
    hosted_at: HostedAt400,
    energy_source: EnergySource400,
    resource_docs_requires_role: Boolean,
    message: String
  )

  def getApiInfoJSON(apiVersion: ApiVersion, message: String): APIInfoJsonV700 = {
    val organisation = APIUtil.getPropsValue("hosted_by.organisation", "TESOBE")
    val email = APIUtil.getPropsValue("hosted_by.email", "contact@tesobe.com")
    val phone = APIUtil.getPropsValue("hosted_by.phone", "+49 (0)30 8145 3994")
    val organisationWebsite = APIUtil.getPropsValue("organisation_website", "https://www.tesobe.com")
    val hostedBy = new HostedBy400(organisation, email, phone, organisationWebsite)

    val organisationHostedAt = APIUtil.getPropsValue("hosted_at.organisation", "")
    val organisationWebsiteHostedAt = APIUtil.getPropsValue("hosted_at.organisation_website", "")
    val hostedAt = HostedAt400(organisationHostedAt, organisationWebsiteHostedAt)

    val organisationEnergySource = APIUtil.getPropsValue("energy_source.organisation", "")
    val organisationWebsiteEnergySource = APIUtil.getPropsValue("energy_source.organisation_website", "")
    val energySource = EnergySource400(organisationEnergySource, organisationWebsiteEnergySource)

    val connector = code.api.Constant.CONNECTOR.openOrThrowException(s"$MandatoryPropertyIsNotSet. The missing prop is `connector` ")
    val resourceDocsRequiresRole = APIUtil.getPropsAsBoolValue("resource_docs_requires_role", false)

    APIInfoJsonV700(
      version = apiVersion.vDottedApiVersion,
      version_status = "BLEEDING_EDGE",
      git_commit = gitCommit,
      connector = connector,
      hostname = Constant.HostName,
      stage = System.getProperty("run.mode"),
      local_identity_provider = Constant.localIdentityProvider,
      hosted_by = hostedBy,
      hosted_at = hostedAt,
      energy_source = energySource,
      resource_docs_requires_role = resourceDocsRequiresRole,
      message = message
    )
  }
}

