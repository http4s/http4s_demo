package org.http4s.example


import java.util.concurrent.Executors

import org.http4s.dsl._
import org.http4s.server.HttpService
import org.http4s.websocket._
import org.http4s.server.websocket.WS

import scala.concurrent.duration._

import scalaz.stream.Process
import scalaz.stream.async.topic

import com.typesafe.scalalogging.slf4j.LazyLogging

class Routes  extends LazyLogging {

  private val cache = new ResourceCache
  private implicit val scheduledEC = Executors.newScheduledThreadPool(1)

  // Provides the message board for our websocket chat
  private val chatTopic = topic[String]()

  val service: HttpService = {

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
  }

}
