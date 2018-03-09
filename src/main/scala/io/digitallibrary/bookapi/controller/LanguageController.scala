/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.service.ReadService
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}


trait LanguageController {
  this: ReadService =>
  val languageController: LanguageController

  class LanguageController (implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for retrieving all languages from the GDL"

    registerModel[api.Error]
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getAllLanguages = (apiOperation[Seq[api.Language]]("getAllLanguages")
      summary s"Returns all languages with content in GDL"
      notes s"Returns all languages with content in GDL"
      parameters headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted.")
      responseMessages response500)

    get("/", operation(getAllLanguages)) {
      readService.listAvailablePublishedLanguages
    }
  }

}
