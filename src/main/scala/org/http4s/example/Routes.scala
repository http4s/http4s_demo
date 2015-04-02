package org.http4s.example


import java.util.concurrent.Executors

import org.http4s.dsl._
import org.http4s.server.HttpService
import org.http4s.websocket.WebsocketBits._
import org.http4s.server.websocket.WS

import scala.concurrent.duration._

import scalaz.stream.Process
import scalaz.stream.async.topic

class Routes {
  private val cache = new ResourceCache
  private implicit val scheduledEC = Executors.newScheduledThreadPool(1)

  // Provides the message board for our websocket chat
  private val chatTopic = topic[String]()

  val service: HttpService = HttpService {

    /** Working with websockets is simple with http4s */

    case r @ GET -> Root / "websocket" =>
      // Send a ping every second
      val src = Process.awakeEvery(1.seconds).map(d => Text("Delay -> " + d))

      WS(src)

    case r @ GET -> Root / "wschat" / name =>

      def frameToMsg(f: WebSocketFrame) = f match {
        case Text(msg, _) => s"$name says: $msg"
        case _            => s"$name sent bad message! Booo..."
      }

      chatTopic.publishOne(s"New user '$name' joined chat")
      .flatMap {_ =>
        val src = Process.emit(Text(s"Welcome to the chat, $name!")) ++ chatTopic.subscribe.map(Text(_))
        val snk = chatTopic.publish.map(_ compose frameToMsg)
          .onComplete(Process.await(chatTopic.publishOne(s"$name left the chat"))(_ => Process.halt))

        WS(src, snk)
      }

    /* Routes for getting static resources. These might be served more efficiently by apache2 or nginx,
     * but its nice to keep the demo self contained */

    case r if r.pathInfo.startsWith("/static") => cache.getResource("", r.pathInfo, r)

    case r if r.pathInfo.startsWith("/swagger") => cache.getResource("", r.pathInfo, r)

    case r @ GET -> Root / path => cache.getResource("/staticviews", if(path.contains('.')) path else path + ".html", r)

    case r if r.pathInfo.endsWith("/") =>
      service(r.withPathInfo(r.pathInfo + "index.html")).map(_.getOrElse(NotFound(r.pathInfo).run))
  }

}
