package org.http4s.example

import org.http4s.rho._
import org.http4s.rho.swagger._

import scalaz.concurrent.Task
import scalaz.stream.Process

import rl.UrlCodingUtils.urlDecode


class RhoRoutes extends RhoService with SwaggerSupport {
  // This is needed for the routes that return json4s `JValues`
  import org.http4s.json4s.jackson.Json4sJacksonSupport._

  "Just a friendly hello route" **
    GET / "hello" |>> { () => Ok("Hello world!") }

  "An XHR example that just echos a result" **
    GET / "things" / *  |>> { (rest: Seq[String]) =>
      Ok(s"Calculating the rest: ${rest.map(urlDecode(_)).mkString("/")}")
    }

  "Data fetching route for models of android phones" **
    GET / "data" / 'id |>> { (id: String) =>
      val data = if (id == "all") Data.getPhones()
                 else Data.getPhonesMatching(id)
      Ok(data)
    }

  "Scalaz-stream makes it simple to compose routes, and cleanup resources" **
    GET / "cleanup" |>> {
      val stream = Process.constant("foo ")
        .take(40)
        .onComplete(Process.eval_(Task{logger.info("Finished!")}))

      Ok(stream)
    }
}
