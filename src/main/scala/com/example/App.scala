package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val fBinding: Promise[ServerBinding] = Promise[ServerBinding]

  def main(args: Array[String]): Unit = {
    val userRoutes: Route = {
      (path("hello" / IntNumber) & get) { i =>
        complete(s"response $i")
      }
    }

    fBinding.completeWith(startHttpServer(userRoutes))

    val poolClientFlow = Http().cachedHostConnectionPool[Unit]("localhost", 8080)

    val (queue, streamCompletion) = Source.queue(10, OverflowStrategy.backpressure)
      .viaMat(poolClientFlow)(Keep.left)
      .mapAsync(5) {
        case (Success(response), _) => Unmarshal(response).to[String].map(println(_))
      }
      .toMat(Sink.ignore)(Keep.both)
      .run()

    def produce(current: Int): Future[Unit] =
      if (current > 10000) {
        Future.successful(())
      } else {
        val offered = queue.offer((HttpRequest(uri = s"/hello/$current"), ()))
        offered.flatMap { r =>
          println(r)
          produce(current + 1)
        }
      }

    produce(0)
      .transformWith(_ => {
        queue.complete(); streamCompletion
      })
      .transformWith(_ => fBinding.future)
      .flatMap(stop)
  }

  private def startHttpServer(routes: Route)(implicit system: ActorSystem): Future[ServerBinding] = {
    Http()
      .newServerAt("localhost", 8080)
      .bind(routes)
      .transform {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
          Success(binding)
        case Failure(ex) =>
          system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
          system.terminate()
          Failure(ex)
      }
  }

  def stop(b: ServerBinding) = {
    b.terminate(5 seconds).andThen {
      case _ => system.terminate()
    }
  }
}
