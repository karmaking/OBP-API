package code.api.v7_0_0

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import code.api.util.{APIUtil, CustomJsonFormats}
import code.api.v4_0_0.JSONFactory400
import code.bankconnectors.Connector
import com.openbankproject.commons.util.{ApiVersion, ScannedApiVersion}
import net.liftweb.json.Formats
import net.liftweb.json.JsonAST.prettyRender
import net.liftweb.json.Extraction
import org.http4s._
import org.http4s.dsl.io._
import org.typelevel.vault.Key

import scala.concurrent.Future
import scala.language.{higherKinds, implicitConversions}

object Http4s700 {

  implicit val formats: Formats = CustomJsonFormats.formats
  implicit def convertAnyToJsonString(any: Any): String = prettyRender(Extraction.decompose(any))

  val apiVersion: ScannedApiVersion = ApiVersion.v7_0_0
  val apiVersionString: String = apiVersion.toString

  case class CallContext(userId: String, requestId: String)
  import cats.effect.unsafe.implicits.global
  val callContextKey: Key[CallContext] = Key.newKey[IO, CallContext].unsafeRunSync()

  object CallContextMiddleware {

    def withCallContext(routes: HttpRoutes[IO]): HttpRoutes[IO] = Kleisli { req: Request[IO] =>
      val callContext = CallContext(userId = "example-user", requestId = java.util.UUID.randomUUID().toString)
      val updatedAttributes = req.attributes.insert(callContextKey, callContext)
      val updatedReq = req.withAttributes(updatedAttributes)
      routes(updatedReq)
    }
  }

  val v700Services: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "obp" / `apiVersionString` / "root" =>
      import com.openbankproject.commons.ExecutionContext.Implicits.global
      val callContext = req.attributes.lookup(callContextKey).get.asInstanceOf[CallContext]
      Ok(IO.fromFuture(IO(
        for {
          _ <- Future() // Just start async call
        } yield {
          convertAnyToJsonString(
            JSONFactory700.getApiInfoJSON(apiVersion, s"Hello, ${callContext.userId}! Your request ID is ${callContext.requestId}.")
          )
        }
      )))

    case req @ GET -> Root / "obp" / `apiVersionString` / "banks" =>
      import com.openbankproject.commons.ExecutionContext.Implicits.global
      Ok(IO.fromFuture(IO(
        for {
          (banks, callContext) <- code.api.util.NewStyle.function.getBanks(None)
        } yield {
          convertAnyToJsonString(JSONFactory400.createBanksJson(banks))
        }
      )))
  }

  val wrappedRoutesV700Services: HttpRoutes[IO] = CallContextMiddleware.withCallContext(v700Services)
}

