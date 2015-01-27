package org.http4s.example

import scala.collection.mutable
import org.http4s.{MediaType, Response, Request}
import scalaz.concurrent.Task
import scalaz.stream.Process
import org.http4s.headers.{`If-Modified-Since`, `Last-Modified`, `Content-Type`}
import scalaz.stream.io.chunkR

import org.log4s.getLogger

import org.http4s.DateTime
import org.http4s.dsl._

/** The resource cache is really a temporary construct that will cache
  * all the static files used by the demo. It is ill advised as a general
  * file distribution tool because it will place EVERY file in memory.
  */
class ResourceCache {
  private val logger = getLogger

  private val startDate = DateTime.now
  private val cacheMap = new mutable.HashMap[String, Array[Byte]]()

  // This is almost certainly not good enough for sanitization...
  private val sanitize = "\\.\\.".r.replaceAllIn(_: String, ".")

  def getResource(dir: String, name: String, req: Request): Task[Response] =  {
    // If the client suggests they may already have a fresh version, send NotModified
    req.headers.get(`If-Modified-Since`).flatMap { h =>
      val expired = h.date.compare(startDate) < 0
      logger.info(s"${req.uri}: Expired: ${expired}. Request age: ${h.date}, Modified: $startDate")

      if (expired) None
      else Some(NotModified())
    }.getOrElse {
      getResourceFromCache(dir, name)
        .putHeaders(`Last-Modified`(startDate))
    }
  }

  ////////////// private methods //////////////////////////////////////////////

  private def checkResource(path: String): Option[Array[Byte]] = {
    // See if we have a minified version of js available first
    val rs = if (path.endsWith(".js")) {
      val min = path.substring(0, path.length - 3) + ".min.js"
      Option(getClass.getResourceAsStream(sanitize(min))) orElse
      Option(getClass.getResourceAsStream(sanitize(path)))
    } else Option(getClass.getResourceAsStream(sanitize(path)))

    rs.map { p =>
      val bytes = Process.constant(8*1024)
        .toSource
        .through(chunkR(p))
        .runLog
        .run
        .map(_.toArray)
        .toArray
        .flatten

      cacheMap(path) = bytes
      bytes
    }
  }

  private def assemblePath(path: String, name: String): String = {
    val realname = if (name.startsWith("/")) name.substring(1) else name
    s"$path/$realname"
  }

  private def getResourceFromCache(dir: String, name: String): Task[Response] = {
    val path = assemblePath(dir, name)
    cacheMap.synchronized {
      cacheMap.get(path) orElse checkResource(path)
    }.fold(NotFound(s"404 Not Found: '$path'")){ bytes =>

      val mime = {
        val parts = path.split('.')
        if (parts.length > 0) MediaType.forExtension(parts.last)
          .getOrElse(MediaType.`application/octet-stream`)
        else MediaType.`application/octet-stream`
      }

      Ok(bytes).putHeaders(`Content-Type`(mime))
    }
  }
}
