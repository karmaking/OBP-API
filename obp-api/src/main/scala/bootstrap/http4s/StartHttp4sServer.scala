package bootstrap.http4s

import cats.data.Kleisli
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._

import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global
import cats.effect._
import com.comcast.ip4s._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.ember.server._


object StartHttp4sServer extends IOApp {
  val helloWorldService: Kleisli[IO, Request[IO], Response[IO]] = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }.orNotFound

  override def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8081")
      .withHttpApp(helloWorldService)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
}
