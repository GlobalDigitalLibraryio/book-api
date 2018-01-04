/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.Language
import no.gdl.bookapi.service.translation.SupportedLanguageService
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait TranslationsController {
  this: SupportedLanguageService =>
  val translationsController: TranslationsController

  class TranslationsController (implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for retrieving all languages from the GDL"

    registerModel[api.Error]
    registerModel[api.ValidationError]

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getSupportedLanguages = (apiOperation[Seq[Language]]("List supported languages")
      summary "Retrieves a list of all supported languages to translate to"
      parameters
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted.")
      responseMessages(response400, response500))

    get("/supported-languages", operation(getSupportedLanguages)) {
      supportedLanguageService.getSupportedLanguages
    }
  }
}
