package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives.{complete, path, _}
import akka.http.scaladsl.server.Route

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Server {

  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val fBinding: Promise[ServerBinding] = Promise[ServerBinding]

  def stop(b: ServerBinding): Future[Http.HttpTerminated] = {
    b.terminate(5 seconds).andThen {
      case _ => system.terminate()
    }
  }

  def startHttpServer(): Future[Unit] = {
    Http()
      .newServerAt("localhost", 8080)
      .bind {
        (path("hello" / IntNumber.optional) & get) { (i: Option[Int]) =>
          complete(s"response $i")
        } ~
          (path("hello") & get) {
            complete(s"hello")
          }
      }
      .andThen {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
          fBinding.success(binding)
        case Failure(ex) =>
          system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
          system.terminate()
          fBinding.failure(ex)
      }
      .map(_ => ())
  }
}
