package org.http4s.example

import org.http4s.rho._
import org.http4s.rho.swagger.SwaggerSupport

import scalaz.concurrent.Task
import scalaz.stream.Process

import rl.UrlCodingUtils.urlDecode


class RhoRoutes extends RhoService with SwaggerSupport {
  import Data.jsonWritable

  GET / "hello" |>> { () => Ok("Hello world!") }

  GET / "things" / *  |>> { (rest: Seq[String]) =>
    Ok(s"Calculating the rest: ${rest.map(urlDecode(_)).mkString("/")}")
  }

  GET / "data" / 'id |>> { (id: String) =>
    val data = if (id == "all") Data.getPhones()
    else Data.getPhonesMatching(id)
    Ok(data)
  }

  /** Scalaz-stream makes it simple to compose routes, and cleanup resources */
  GET / "cleanup" |>> { () =>
    val stream = Process.constant("foo ")
      .take(40)
      .onComplete(Process.await(Task{logger.info("Finished!")})(_ => Process.halt))

    Ok(stream)
  }
}
