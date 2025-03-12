package bootstrap.http4s

import cats.effect.{Concurrent, ContextShift, Timer}
import fs2.Stream
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware.Logger

import scala.language.higherKinds

object Http4sServer {

  def stream[F[_]: Concurrent: ContextShift: Timer]: Stream[F, Nothing] = {
    val helloWorldAlg = HelloWorld.impl[F]

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract a segments not checked
    // in the underlying routes.
    val httpApp = (
//      RestRoutes.helloWorldRoutes[F](helloWorldAlg)
      RestRoutes.getBankRoutes[F]()
      ).orNotFound

//    val httpApp = Router(
//      "/hello" -> RestRoutes.helloWorldRoutes[F](helloWorldAlg),
//      "/banks"  -> RestRoutes.getBankRoutes[F]()
//    ).orNotFound
//    
    // With Middlewares in place
    val finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)
    for {
      exitCode <-  Stream.resource(
        EmberServerBuilder.default[F]
          .withHost("127.0.0.1")
          .withPort(8081)
          .withHttpApp(finalHttpApp)
          .build
      ) >> Stream.never[F]
    } yield exitCode
  }.drain
}
