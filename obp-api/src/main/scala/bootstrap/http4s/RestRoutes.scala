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
import cats.effect._, org.http4s._, org.http4s.dsl.io._


object RestRoutes {

  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }

  case class Tweet(id: Int, message: String)

  implicit def tweetEncoder: EntityEncoder[IO, Tweet] = ???
  implicit def tweetsEncoder: EntityEncoder[IO, Seq[Tweet]] = ???

  def getTweet(tweetId: Int): IO[Tweet] = ???
  def getPopularTweets(): IO[Seq[Tweet]] = ???

  val tweetService = HttpRoutes.of[IO] {
    case GET -> Root / "tweets" / "popular" =>
      getPopularTweets().flatMap(Ok(_))
    case GET -> Root / "tweets" / IntVar(tweetId) =>
      getTweet(tweetId).flatMap(Ok(_))
  }
  
//  def getBankRoutes[F[_]: Sync](): HttpRoutes[F] = {
//    
//    import net.liftweb.json.JsonAST.{JValue, prettyRender}
//    import net.liftweb.json.{Extraction, MappingException, compactRender, parse}
//    implicit val formats: Formats = CustomJsonFormats.formats
//    HttpRoutes.of[F] {
//      case GET -> Root / "banks"  =>
////        val banks = MappedBank.findAll().map(
////          bank =>
////            bank
////              .mBankRoutingScheme(APIUtil.ValueOrOBP(bank.bankRoutingScheme))
////              .mBankRoutingAddress(APIUtil.ValueOrOBPId(bank.bankRoutingAddress, bank.bankId.value)
////              ))
//        val banks2 = Connector.connector.vend.getBanksLegacy(None).map(_._1).openOrThrowException("xxxxx")
////        val banks= List(BankCommons(
////          bankId=com.openbankproject.commons.model.BankId("bankIdExample.value"),
////          shortName="bankShortNameExample.value",
////          fullName="bankFullNameExample.value",
////          logoUrl="bankLogoUrlExample.value",
////          websiteUrl="bankWebsiteUrlExample.value",
////          bankRoutingScheme="bankRoutingSchemeExample.value",
////          bankRoutingAddress="bankRoutingAddressExample.value",
////          swiftBic="bankSwiftBicExample.value",
////          nationalIdentifier="bankNationalIdentifierExample.value")
////        )
//        for {
//          resp <- Ok(prettyRender(Extraction.decompose(banks2)))
//        } yield resp
//    }
//  }
}