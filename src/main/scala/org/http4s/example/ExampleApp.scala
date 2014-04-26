package org.http4s.example

import org.http4s.blaze._
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.blaze.channel.nio1.SocketServerChannelFactory
import org.http4s.blaze.websocket.WebSocketSupport

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.http4s.blaze.channel.nio2.NIO2ServerChannelFactory


/**
 * Created by Bryce Anderson on 3/23/14.
 */

class ExampleApp(addr: InetSocketAddress) {

  private val route = new Routes
  private val pool = Executors.newCachedThreadPool()

  //private val factory = new NIO2ServerChannelFactory(f)
  private val factory = new SocketServerChannelFactory(f, 4, 16*1024)

  def f() = {
    val s = new Http1Stage(route.service)(pool) with WebSocketSupport
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

