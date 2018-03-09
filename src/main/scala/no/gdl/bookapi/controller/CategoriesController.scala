/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api
import no.gdl.bookapi.service.ReadService
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}


trait CategoriesController {
  this: ReadService =>
  val categoriesController: CategoriesController

  class CategoriesController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "TODO"

    val defaultLanguage = LanguageTag(BookApiProperties.DefaultLanguage)

    registerModel[api.Error]
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getCategoriesForLanguage = (apiOperation[Map[String, Seq[String]]]("getCategoriesForLanguage")
      summary "Returns a hierarchy of categories and reading levels of books available in given language"
      description "Returns a hierarchy of categories and reading levels of books available in given language"
      parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-1/2 format."))
      responseMessages response500)

    get("/", operation(getCategoriesForLanguage)) {
      readService.listAvailablePublishedCategoriesForLanguage(defaultLanguage)
    }

    get("/:lang", operation(getCategoriesForLanguage)) {
      val language = LanguageTag(params("lang"))
      readService.listAvailablePublishedCategoriesForLanguage(language)
    }
  }
}
