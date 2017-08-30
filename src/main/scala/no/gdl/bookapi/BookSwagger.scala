/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object BookApiInfo {
  val apiInfo = ApiInfo(
  "Book Api",
  "Documentation for the Book API of GDL",
  "https://ndla.no",
  BookApiProperties.ContactEmail,
  "GPL v3.0",
  "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class BookSwagger extends Swagger("2.0", "0.8", BookApiInfo.apiInfo) {
  addAuthorization(OAuth(List("books:all"), List(ApplicationGrant(TokenEndpoint("/auth/tokens", "access_token")))))
}
