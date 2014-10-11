package org.http4s.example

import org.http4s.Header
import org.http4s.server.HttpService


class HeaderInjector(headers: Header*) {
  def apply(srvc: HttpService): HttpService = srvc.andThen(_.map { resp =>
    resp.putHeaders(headers:_*)
  })
}
