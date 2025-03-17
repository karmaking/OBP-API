package code.api.v1_3_0

import code.api.ResourceDocs1_4_0.SwaggerDefinitionsJSON._
import code.api.util.APIUtil._
import code.api.util.ApiTag._
import code.api.util.ErrorMessages._
import code.api.util.FutureUtil.EndpointContext
import code.api.util.NewStyle.HttpCode
import code.api.util.{ApiRole, NewStyle}
import code.api.v1_2_1.JSONFactory
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model.BankId
import com.openbankproject.commons.util.{ApiVersion, ScannedApiVersion}
import net.liftweb.common.Full
import net.liftweb.http.rest.RestHelper

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
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

import scala.language.{higherKinds, implicitConversions}
import cats.effect._
import code.api.v4_0_0.JSONFactory400
import org.http4s._
import org.http4s.dsl.io._
import net.liftweb.json.JsonAST.{JValue, prettyRender}
import net.liftweb.json.{Extraction, MappingException, compactRender, parse}


object Http4s130 {

  implicit val formats: Formats = CustomJsonFormats.formats
  implicit def convertAnyToJsonString(any: Any): String =  prettyRender(Extraction.decompose(any))
  
  val apiVersion: ScannedApiVersion = ApiVersion.v1_3_0
  
  val v130Services: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / apiVersion /"root"   =>
        Ok(IO.fromFuture(IO(
          for {
            _ <- Future() // Just start async call
          } yield {
            convertAnyToJsonString(
              JSONFactory.getApiInfoJSON(OBPAPI1_3_0.version, OBPAPI1_3_0.versionStatus)
            )
          }
        )))
  }
}
