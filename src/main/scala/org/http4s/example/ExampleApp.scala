package org.http4s.example

import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.server.blaze.{Http1ServerStage, WebSocketSupport}
import org.http4s.blaze.channel.nio1.SocketServerChannelFactory

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.http4s.Request
import org.http4s.server.HttpService
import org.http4s.blaze.channel.SocketConnection

import org.http4s.dsl._


class ExampleApp(addr: InetSocketAddress) extends StrictLogging {

  private val pool = Executors.newCachedThreadPool()

  val routes = new Routes().service

  val service: HttpService =  { case req: Request =>
    val path = req.uri.path
    logger.info(s"${req.remoteAddr.getOrElse("null")} -> ${req.method}: $path")

    val resp = routes.applyOrElse(req, {_: Request => NotFound(req.uri.path)})
    resp
  }

  private val factory = new SocketServerChannelFactory(f, 4, 16*1024)

  def f(conn: SocketConnection) = {
    val s = new Http1ServerStage(service, Some(conn))(pool) with WebSocketSupport
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

  def main(args: Array[String]): Unit = new ExampleApp(new InetSocketAddress(ip, port)).run()
}

