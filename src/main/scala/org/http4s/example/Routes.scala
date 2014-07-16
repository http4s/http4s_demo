package org.http4s.example

import org.http4s.dsl._
import org.http4s.server.HttpService
import org.http4s.Status.{NotFound, Ok}
import org.http4s.server.websocket._

import scala.concurrent.duration._

import scalaz.concurrent.Task
import scalaz.stream.Process
import scalaz.stream.async.topic

import com.typesafe.scalalogging.slf4j.LazyLogging

class Routes  extends LazyLogging {

  import Data.jsonWritable

  val cache = new ResourceCache

  // Provides the message board for our websocket chat
  val chatTopic = topic[String]()

  val service: HttpService = {
    case GET -> Root / "hello" => Ok("Hello world!")

    case GET -> Root / "things" / rest => Ok(s"Calculating the rest: $rest")

    case GET -> Root / "data" / id =>
      val data = if (id == "all") Data.getPhones()
                 else Data.getPhonesMatching(id)
      Ok(data)

    /** Scalaz-stream makes it simple to compose routes, and cleanup resources */
    case GET -> Root / "cleanup" =>
      val d = Process.constant("foo ")
          .take(40)
          .onComplete(Process.await(Task{logger.info("Finished!")})(_ => Process.halt))

      Ok(d)

    /** Working with websockets is simple with http4s */

    case r @ GET -> Root / "websocket" =>
      // Send a ping every second
      val src = Process.awakeEvery(1.seconds).map(d => Text("Delay -> " + d))

      WS(src)

    case r @ GET -> Root / "wschat" / name =>

      def frameToMsg(f: WSFrame) = f match {
        case Text(msg) => s"$name says: $msg"
        case _ =>         s"$name sent bad message! Booo..."
      }

      chatTopic.publishOne(s"New user '$name' joined chat")
      .flatMap {_ =>
        val src = Process.emit(Text(s"Wecome to the chat, $name!")) ++ chatTopic.subscribe.map(Text(_))
        val snk = chatTopic.publish.map(_ compose frameToMsg)
          .onComplete(Process.await(chatTopic.publishOne(s"$name left the chat"))(_ => Process.halt))

        WS(src, snk)
      }

    /** Routes for getting static resources. These might be served more efficiently by apache2 or nginx,
      * but its nice to keep it self contained
      */

    case r if r.pathInfo.startsWith("/static") => cache.getResource("", r.pathInfo, r)

    case r @ GET -> Root / path => cache.getResource("/staticviews", if(path.contains('.')) path else path + ".html", r)

    case r if r.pathInfo.endsWith("/") => service(r.withPathInfo(r.pathInfo + "index.html"))

    case r => NotFound("404 Not Found: " + r.pathInfo)
  }

}
