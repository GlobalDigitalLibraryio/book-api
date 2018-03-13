/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.model.api.ReadingLevels
import io.digitallibrary.bookapi.service.ReadService
import io.digitallibrary.language.model.LanguageTag
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}


trait CategoriesController {
  this: ReadService =>
  val categoriesController: CategoriesController

  class CategoriesController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for getting a hierarchy of categories and reading levels of published books."

    val defaultLanguage = LanguageTag(BookApiProperties.DefaultLanguage)

    registerModel[api.Error]
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getCategoriesForLanguage = (apiOperation[Map[String, ReadingLevels]]("getCategoriesForLanguage")
      summary "Returns a hierarchy of categories and reading levels of books available in given language"
      description "Returns a hierarchy of categories and reading levels of books available in given language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-1/2 format."))
      responseMessages response500)

    get("/", operation(getCategoriesForLanguage)) {
      getCategoriesFor(defaultLanguage)
    }

    get("/:lang", operation(getCategoriesForLanguage)) {
      getCategoriesFor(LanguageTag(params("lang")))
    }

    def getCategoriesFor(language: LanguageTag): Map[String, ReadingLevels] = {
      readService.listAvailablePublishedCategoriesForLanguage(language).map { case (category, readingLevels) =>
        category.name -> ReadingLevels(readingLevels)
      }
    }

  }

}
