//package bootstrap.http4s
//
//import cats._
//import cats.effect._
//import cats.implicits._
//import cats.data._
//import code.api.util.CallContext
//import org.http4s._
//import org.http4s.dsl.io._
//import org.http4s.server._
//
//object Middleware {
//
//  val authUser: Kleisli[OptionT[IO, *], Request[IO], CallContext] =
//    Kleisli(_ => OptionT.liftF(IO(???)))
//
//  val middleware: AuthMiddleware[IO, CallContext] = AuthMiddleware(authUser)
//
//  val authedRoutes: AuthedRoutes[CallContext, IO] =
//    AuthedRoutes.of {
//      case GET -> Root / "welcome" as callContext => Ok(s"Welcome, ${callContext}")
//    }
//
//  val service: HttpRoutes[IO] = middleware(authedRoutes)
//
//}
