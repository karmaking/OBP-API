package bootstrap.http4s

import cats.effect.Sync
import cats.implicits._
import code.api.util.{APIUtil, CustomJsonFormats}
import code.bankconnectors.Connector
import code.model.dataAccess.MappedBank
import com.openbankproject.commons.model.BankCommons
import net.liftweb.json.Formats
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import scala.language.higherKinds

object RestRoutes {

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }
  
  def getBankRoutes[F[_]: Sync](): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    import net.liftweb.json.JsonAST.{JValue, prettyRender}
    import net.liftweb.json.{Extraction, MappingException, compactRender, parse}
    implicit val formats: Formats = CustomJsonFormats.formats
    HttpRoutes.of[F] {
      case GET -> Root / "banks"  =>
//        val banks = MappedBank.findAll().map(
//          bank =>
//            bank
//              .mBankRoutingScheme(APIUtil.ValueOrOBP(bank.bankRoutingScheme))
//              .mBankRoutingAddress(APIUtil.ValueOrOBPId(bank.bankRoutingAddress, bank.bankId.value)
//              ))
        val banks2 = Connector.connector.vend.getBanksLegacy(None).map(_._1).openOrThrowException("xxxxx")
//        val banks= List(BankCommons(
//          bankId=com.openbankproject.commons.model.BankId("bankIdExample.value"),
//          shortName="bankShortNameExample.value",
//          fullName="bankFullNameExample.value",
//          logoUrl="bankLogoUrlExample.value",
//          websiteUrl="bankWebsiteUrlExample.value",
//          bankRoutingScheme="bankRoutingSchemeExample.value",
//          bankRoutingAddress="bankRoutingAddressExample.value",
//          swiftBic="bankSwiftBicExample.value",
//          nationalIdentifier="bankNationalIdentifierExample.value")
//        )
        for {
          resp <- Ok(prettyRender(Extraction.decompose(banks2)))
        } yield resp
    }
  }
}