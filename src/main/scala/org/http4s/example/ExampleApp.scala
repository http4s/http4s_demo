package org.http4s.example


import java.net.InetSocketAddress
import java.util.concurrent.Executors

import org.http4s.blaze.channel.SocketConnection
import org.http4s.blaze.channel.nio1.{NIO1SocketServerChannelFactory}
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.server.blaze.{WebSocketSupport, Http1ServerStage}
import org.http4s.server.HttpService

import scala.util.Properties.envOrNone

import org.log4s.getLogger




class ExampleApp(addr: InetSocketAddress) {
  private val logger = getLogger
  private val pool = Executors.newCachedThreadPool()

  logger.info(s"Starting Http4s-blaze example on '$addr'")

  // build our routes
  def rhoRoutes = new RhoRoutes()

  // our routes can be combinations of any HttpService, regardless of where they come from
  val routes = rhoRoutes.toService orElse new Routes().service

  // Add some logging to the service
  val service: HttpService = routes.contramap { req =>
    val path = req.uri.path
    logger.info(s"${req.remoteAddr.getOrElse("null")} -> ${req.method}: $path")
    req
  }

  def run(): Unit = {
    // Construct the blaze pipeline. We could use `import org.http4s.server.blaze.BlazeServer`
    // which is much cleaner, except that we need to include websocket support.
    def pipelineBuilder(conn: SocketConnection) = {
      val s = new Http1ServerStage(service, Some(conn), pool) with WebSocketSupport
      LeafBuilder(s)
    }

    new NIO1SocketServerChannelFactory(pipelineBuilder, 4, 16*1024)
      .bind(addr)
      .run()
  }
}

object ExampleApp {
  val ip =   envOrNone("OPENSHIFT_DIY_IP").getOrElse("0.0.0.0")
  val port = envOrNone("OPENSHIFT_DIY_PORT") orElse envOrNone("HTTP_PORT") map(_.toInt) getOrElse(8080)

  def main(args: Array[String]): Unit = {
    new ExampleApp(new InetSocketAddress(ip, port)).run()
  }
}

