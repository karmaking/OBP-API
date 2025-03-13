package bootstrap.http4s

import cats.effect._
import org.http4s.{HttpRoutes, _}
import org.http4s.dsl.io._
import cats.implicits._
import code.api.util.{APIUtil, CustomJsonFormats}
import code.bankconnectors.Connector
import code.model.dataAccess.MappedBank
import com.openbankproject.commons.model.BankCommons
import net.liftweb.json.Formats
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds
import cats.effect._, org.http4s._, org.http4s.dsl.io._
import net.liftweb.json.JsonAST.{JValue, prettyRender}
import net.liftweb.json.{Extraction, MappingException, compactRender, parse}

object RestRoutes {
  implicit val formats: Formats = CustomJsonFormats.formats
  
  val helloWorldService: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }

  val bankServices: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "banks"  =>
      val banks = Connector.connector.vend.getBanksLegacy(None).map(_._1).openOrThrowException("xxxxx")
      Ok(prettyRender(Extraction.decompose(banks)))
    case GET -> Root / "banks" / IntVar(bankId) =>
      val bank = BankCommons(
        bankId = com.openbankproject.commons.model.BankId("bankIdExample.value"),
        shortName = "bankShortNameExample.value",
        fullName = "bankFullNameExample.value",
        logoUrl = "bankLogoUrlExample.value",
        websiteUrl = "bankWebsiteUrlExample.value",
        bankRoutingScheme = "bankRoutingSchemeExample.value",
        bankRoutingAddress = "bankRoutingAddressExample.value",
        swiftBic = "bankSwiftBicExample.value",
        nationalIdentifier = "bankNationalIdentifierExample.value")
      Ok(prettyRender(Extraction.decompose(bank)))
  }
  
}