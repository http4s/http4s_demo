package org.http4s.example

import scala.collection.mutable
import org.http4s.{MediaType, Response, Request}
import scalaz.concurrent.Task
import scalaz.stream.Process
import org.http4s.Header.{`If-Modified-Since`, `Last-Modified`, `Content-Type`}
import scalaz.stream.io.chunkR
import com.typesafe.scalalogging.slf4j.StrictLogging

import org.http4s.Http4s._
import org.joda.time.DateTime

/**
 * Created by Bryce Anderson on 4/12/14.
 */
class ResourceCache extends StrictLogging {

  private val startDate = new DateTime().millisOfSecond().setCopy(0)

  private val cacheMap = new mutable.HashMap[String, Array[Byte]]()

  private val sanitizeRegex = "\\.\\.".r

  // This is almost certainly not good enough for sanitization...
  private def sanitize(path: String): String = sanitizeRegex.replaceAllIn(path, ".")

  private def checkResource(path: String): Option[Array[Byte]] = {
    // See if we have a minified version of js available first
    val rs = if (path.endsWith(".js") && false) {
      val min = path.substring(0, path.length - 3) + ".min.js"
      Option(getClass.getResourceAsStream(sanitize(min))) orElse
      Option(getClass.getResourceAsStream(sanitize(path)))
    } else Option(getClass.getResourceAsStream(sanitize(path)))

    rs.map { p =>
      val bytes = Process.constant(8*1024)
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
    val realdir = if (path != "") path else ""
    val realname = if (name.startsWith("/")) name.substring(1) else name
    s"$realdir/$realname"
  }

  private def _getResource(dir: String, name: String): Task[Response] = {
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

  def getResource(dir: String, name: String, req: Request): Task[Response] =  {
    // If the client suggests they may already have a fresh version, send NotModified
    req.headers.get(`If-Modified-Since`).flatMap { h =>
      val expired = h.date.compareTo(startDate) < 0
      logger.info(s"${req.requestUri}: Expired: ${expired}. Request age: ${h.date}, Modified: $startDate")

      if (expired) None
      else Some(NotModified())
    }.getOrElse {
      _getResource(dir, name)
        .addHeaders(`Last-Modified`(startDate))
    }
  }

}
