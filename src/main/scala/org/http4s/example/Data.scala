package org.http4s.example

import java.nio.charset.StandardCharsets

import org.http4s.Writable.Entity
import org.json4s._
import org.json4s.native.JsonMethods._
import scodec.bits.ByteVector
import org.http4s.{Headers, MediaType, Writable}
import org.http4s.Header.`Content-Type`

import scalaz.concurrent.Task
import scalaz.stream.Process.emit

object Data {

  // This of this as a very small and unchanging database...
  val JArray(phones) = parse(s"""[{"name": "Nexus S",
                   |     "snippet": "Fast just got faster with Nexus S.",
                   |     "img": "https://raw.githubusercontent.com/angular/angular-phonecat/master/app/img/phones/nexus-s.2.jpg" },
                   |    {"name": "Motorola XOOM™ with Wi-Fi",
                   |     "snippet": "The Next, Next Generation tablet.",
                   |     "img": "https://raw.githubusercontent.com/angular/angular-phonecat/master/app/img/phones/motorola-xoom-with-wi-fi.0.jpg"},
                   |    {"name": "MOTOROLA XOOM™",
                   |     "snippet": "The Next, Next Generation tablet.",
                   |     "img": "https://raw.githubusercontent.com/angular/angular-phonecat/master/app/img/phones/motorola-xoom.0.jpg"}]""".stripMargin)

  def getPhones() = JArray(phones)
  def getPhonesMatching(search: String) = JArray {
    phones.filter {
      case JObject(JField("name", JString(name))::_) if name.contains(search) => true
      case _ => false
    }
  }

  /** You can make a Writable for any custom data type you want!
    * In the future, macro Writables will make make it simple to turn
    * case classes into a form encoded body or json, etc.
    */
  implicit val jsonWritable = new Writable[JValue]( jv => {
    val bv = ByteVector.view(compact(render(jv)).getBytes(StandardCharsets.UTF_8))
    Task.now(Entity(emit(bv), Some(bv.length)))
  }, Headers(`Content-Type`(MediaType.`application/json`)))

}
