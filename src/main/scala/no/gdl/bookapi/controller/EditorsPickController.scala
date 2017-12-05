/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties.DefaultLanguage
import no.gdl.bookapi.model.api
import no.gdl.bookapi.service.ReadService
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}


trait EditorsPickController {
  this: ReadService =>
  val editorsPickController: EditorsPickController

  class EditorsPickController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for retrieving Editors chosen books from GDL"

    registerModel[api.Error]
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getAllPicks = (apiOperation[Seq[api.Book]]("getAllPicks")
      summary s"Returns all editors picks in $DefaultLanguage"
      notes s"Returns all editors picks in $DefaultLanguage"
      parameters headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted.")
      authorizations "oauth2"
      responseMessages response500)

    private val getAllPicksInLang = (apiOperation[Seq[api.Book]]("getAllPicksInLang")
      summary s"Returns all editors picks in given language"
      notes s"Returns all editors picks in given language"
      parameters (headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-1/2 format."))
      authorizations "oauth2"
      responseMessages response500)

    get("/", operation(getAllPicks)) {
      readService.editorsPickForLanguage(LanguageTag(DefaultLanguage))
        .map(_.books)
        .getOrElse(Seq())
    }

    get("/:lang", operation(getAllPicksInLang)) {
      readService.editorsPickForLanguage(LanguageTag(params("lang")))
        .map(_.books)
        .getOrElse(Seq())
    }
  }

}
