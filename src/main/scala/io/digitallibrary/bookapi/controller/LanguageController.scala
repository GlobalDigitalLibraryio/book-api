/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.model.api
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.service.{ConverterService, ReadService}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}


trait LanguageController {
  this: ReadService with ConverterService =>
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

    private val getLanguage = (apiOperation[api.Language]("getLanguage")
      summary s"Return the language for the given language code."
      notes s"Return the language for the given language code."
      parameters (
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        pathParam[String]("lang").description("Desired language specified in ISO 639-2 format.")
      )
      responseMessages response500)

    get("/:lang", operation(getLanguage)) {
      var language = LanguageTag(params("lang"))
      converterService.toApiLanguage(language)
    }
  }

}
