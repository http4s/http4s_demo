package org.http4s.example

import org.json4s._
import org.json4s.jackson.JsonMethods._

/** This is our "database" */
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
}
