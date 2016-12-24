package org.http4s.example


import java.util.concurrent.Executors

import org.http4s.dsl._
import org.http4s.websocket.WebsocketBits._

import org.http4s.HttpService
import org.http4s.server.staticcontent
import org.http4s.server.staticcontent.ResourceService.Config
import org.http4s.server.websocket.WS


import scala.concurrent.duration._

import scalaz.stream.{Exchange, Process, time}
import scalaz.stream.async.topic

class Routes {
  private implicit val scheduledEC = Executors.newScheduledThreadPool(4)

  // Provides the message board for our websocket chat
  private val chatTopic = topic[String]()

  // Get the static content
  private val static  = cachedResource(Config("/static", "/static"))
  private val views   = cachedResource(Config("/staticviews", "/"))
  private val swagger = cachedResource(Config("/swagger", "/swagger"))

  val service: HttpService = HttpService {

    /** Working with websockets is simple with http4s */

    case r @ GET -> Root / "websocket" =>
      // Send a ping every second
      val src = time.awakeEvery(1.seconds).map(d => Text("Delay -> " + d))

      WS(Exchange(src, Process.halt))

    case r @ GET -> Root / "wschat" / name =>

      def frameToMsg(f: WebSocketFrame) = f match {
        case Text(msg, _) => s"$name says: $msg"
        case _            => s"$name sent bad message! Booo..."
      }

      chatTopic.publishOne(s"New user '$name' joined chat")
      .flatMap {_ =>
        val src = Process.emit(Text(s"Welcome to the chat, $name!")) ++ chatTopic.subscribe.map(Text(_))
        val snk = chatTopic.publish.contramap(frameToMsg)
          .onComplete(Process.eval_(chatTopic.publishOne(s"$name left the chat")))

        WS(Exchange(src, snk))
      }

    /* Routes for getting static resources. These might be served more efficiently by apache2 or nginx,
     * but its nice to keep the demo self contained */

    case r @ GET -> _ if r.pathInfo.startsWith("/static") => static(r)

    case r @ GET -> _ if r.pathInfo.startsWith("/swagger") => swagger(r)

    case r @ GET -> _ if r.pathInfo.endsWith("/") => service(r.withPathInfo(r.pathInfo + "index.html"))

    case r @ GET -> _ =>
      val rr = if (r.pathInfo.contains('.')) r else r.withPathInfo(r.pathInfo + ".html")
      views(rr)
  }
  
  private def cachedResource(config: Config): HttpService = {
    val cachedConfig = config.copy(cacheStrategy = staticcontent.MemoryCache())
    staticcontent.resourceService(cachedConfig)
  }
}
