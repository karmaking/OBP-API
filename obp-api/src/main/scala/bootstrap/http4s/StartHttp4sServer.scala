package bootstrap.http4s

import cats.effect.{ExitCode, IO, IOApp}

object StartHttp4sServer extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
     Http4sServer.stream[IO].compile.drain.as(ExitCode.Success)
}
