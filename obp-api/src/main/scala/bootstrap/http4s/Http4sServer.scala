package bootstrap.http4s

import bootstrap.http4s.RestRoutes.{tweetService,helloWorldService}

import scala.language.higherKinds
import cats.syntax.all._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.Router
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import scala.concurrent.duration._
object Http4sServer {

  val services = tweetService <+> helloWorldService
  val httpApp = Router("/" -> helloWorldService, "/api" -> services).orNotFound
  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(httpApp)
    .build
  

}
