package org.http4s.example


import java.net.InetSocketAddress
import java.util.concurrent.Executors

import scala.util.Properties.envOrNone

import com.typesafe.scalalogging.slf4j.StrictLogging

import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.blaze.channel.nio1.SocketServerChannelFactory
import org.http4s.blaze.channel.SocketConnection
import org.http4s.server.blaze.{Http1ServerStage, WebSocketSupport}

import org.http4s.server.HttpService
import org.http4s.{Header, Request}
import org.http4s.dsl._


class ExampleApp(addr: InetSocketAddress) extends StrictLogging {

  private val pool = Executors.newCachedThreadPool()

  // build our routes
  def rhoRoutes = new HeaderInjector(Header("Access-Control-Allow-Origin", "*"))
                       .apply(new RhoRoutes())

  // our routes can be combinations of any HttpService, regardless of where they come from
  val routes = rhoRoutes orElse new Routes().service

  // Add some logging to the service
  val service: HttpService =  { case req: Request =>
    val path = req.uri.path
    logger.info(s"${req.remoteAddr.getOrElse("null")} -> ${req.method}: $path")

    val resp = routes.applyOrElse(req, {_: Request => NotFound(req.uri.path)})
    resp
  }

  // construct the blaze pipeline. We could use `import org.http4s.server.blaze.BlazeServer`
  // except that we need to include websocket support
  def pipelineBuilder(conn: SocketConnection) = {
    val s = new Http1ServerStage(service, Some(conn))(pool) with WebSocketSupport
    LeafBuilder(s)
  }

  def run(): Unit = new SocketServerChannelFactory(pipelineBuilder, 4, 16*1024)
                          .bind(addr)
                          .run()
}

object ExampleApp extends StrictLogging {
  val ip =   envOrNone("OPENSHIFT_DIY_IP").getOrElse("0.0.0.0")
  val port = envOrNone("OPENSHIFT_DIY_PORT") orElse envOrNone("HTTP_PORT") map(_.toInt) getOrElse(8080)

  def main(args: Array[String]): Unit = {
    logger.info(s"Starting Http4s-blaze example on '$ip:$port'")
    new ExampleApp(new InetSocketAddress(ip, port)).run()
  }
}

