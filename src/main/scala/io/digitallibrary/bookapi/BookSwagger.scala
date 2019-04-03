/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi

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
  "https://digitallibrary.io",
  BookApiProperties.ContactEmail,
  "Apache License 2.0",
  "https://www.apache.org/licenses/LICENSE-2.0")
}

class BookSwagger extends Swagger("2.0", "0.8", BookApiInfo.apiInfo) {
  addAuthorization(OAuth(List("books:all"), List(ImplicitGrant(LoginEndpoint(BookApiProperties.LoginEndpoint), "access_token"))))
}