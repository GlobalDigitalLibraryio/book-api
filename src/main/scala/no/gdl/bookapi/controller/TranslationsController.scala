/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.{Language, TranslateRequest, TranslateResponse}
import no.gdl.bookapi.service.translation.{SupportedLanguageService, TranslationService}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait TranslationsController {
  this: SupportedLanguageService with TranslationService =>
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

    private val sendResourceToTranslation = (apiOperation[TranslateResponse]("Send book to translation")
      summary "Sends a book to translation system"
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      bodyParam[TranslateRequest])
      responseMessages(response400, response500)
      authorizations "oauth2")

    get("/supported-languages", operation(getSupportedLanguages)) {
      supportedLanguageService.getSupportedLanguages
    }

    post("/", operation(sendResourceToTranslation)) {
      requireUser
      translationService.addTranslation(extract[TranslateRequest](request.body))
    }
  }
}
