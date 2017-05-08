/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object ReadingMaterialsApiInfo {
  val apiInfo = ApiInfo(
  "Reading Materials Api",
  "Documentation for the Reading Materials API of GDL",
  "https://ndla.no",
  ReadingMaterialsApiProperties.ContactEmail,
  "GPL v3.0",
  "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class ReadingMaterialsSwagger extends Swagger("2.0", "0.8", ReadingMaterialsApiInfo.apiInfo) {
  addAuthorization(OAuth(List("reading-materials:all"), List(ApplicationGrant(TokenEndpoint("/auth/tokens", "access_token")))))
}
