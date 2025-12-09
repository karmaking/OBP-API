package bootstrap.http4s

import cats.data.{Kleisli, OptionT}

import scala.language.higherKinds
import cats.syntax.all._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.implicits._
import cats.effect._
import code.api.util.APIUtil
import org.http4s._
object Http4sServer extends IOApp {

  val services: Kleisli[({type λ[β$0$] = OptionT[IO, β$0$]})#λ, Request[IO], Response[IO]] = 
      code.api.v7_0_0.Http4s700.wrappedRoutesV700Services
      
  val httpApp: Kleisli[IO, Request[IO], Response[IO]] = (services).orNotFound

  //Start OBP relevant objects, and settings
  new bootstrap.liftweb.Boot().boot

  val port = APIUtil.getPropsAsIntValue("http4s.port",8181)
  val host = APIUtil.getPropsValue("http4s.host","127.0.0.1")
  
  
  override def run(args: List[String]): IO[ExitCode] = EmberServerBuilder
    .default[IO]
    .withHost(Host.fromString(host).get)
    .withPort(Port.fromInt(port).get)
    .withHttpApp(httpApp)
    .build
    .use(_ => IO.never)
    .as(ExitCode.Success)
}

