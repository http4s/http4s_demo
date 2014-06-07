package org.http4s.example

import org.http4s.blaze._
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.blaze.websocket.WebSocketSupport
import org.http4s.blaze.channel.nio2.NIO2ServerChannelFactory
import org.http4s.blaze.channel.nio1.SocketServerChannelFactory

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.typesafe.scalalogging.slf4j.StrictLogging
import java.nio.channels.AsynchronousSocketChannel
import org.http4s.{HttpService, Status, Request}
import org.http4s.blaze.channel.SocketConnection


/**
 * Created by Bryce Anderson on 3/23/14.
 */

class ExampleApp(addr: InetSocketAddress) extends StrictLogging {

  private val pool = Executors.newCachedThreadPool()

  val routes = new Routes().service

  val service: HttpService =  { case req: Request =>
    val uri = req.requestUri.path
    logger.info(s"${req.remoteAddr.getOrElse("null")} -> ${req.requestMethod}: ${req.requestUri.path}")

    val resp = routes.applyOrElse(req, {_: Request => Status.NotFound(req)})
    resp
  }

//  private val factory = new NIO2ServerChannelFactory(f) {
//    override protected def acceptConnection(channel: AsynchronousSocketChannel): Boolean = {
//      logger.info(s"New connection: ${channel.getRemoteAddress}")
//      super.acceptConnection(channel)
//    }
//  }
  private val factory = new SocketServerChannelFactory(f, 4, 16*1024)

  def f(conn: SocketConnection) = {
    val s = new Http1Stage(service, Some(conn))(pool) with WebSocketSupport
    LeafBuilder(s)
  }

  def run(): Unit = factory.bind(addr).run()
}

object ExampleApp extends StrictLogging {
  val ip = Option(System.getenv("OPENSHIFT_DIY_IP")).getOrElse("0.0.0.0")
  val port = (Option(System.getenv("OPENSHIFT_DIY_PORT")) orElse
              Option(System.getenv("HTTP_PORT")))
          .map(_.toInt)
          .getOrElse(8080)

  logger.info(s"Starting Http4s-blaze example on '$ip:$port'")
  println(s"Starting Http4s-blaze example on '$ip:$port'")

  def main(args: Array[String]): Unit = new ExampleApp(new InetSocketAddress(ip, port)).run()
}

