/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.service.ReadService
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}


// TODO #221 Remove this when no longer used
trait LevelController {
  this: ReadService =>
  val levelController: LevelController

  class LevelController (implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for retrieving all levels from GDL"

    registerModel[api.Error]
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getAllLevels = (apiOperation[Seq[String]]("getAllLevels")
      summary s"Returns all levels with content in GDL"
      notes s"Returns all levels with content in GDL"
      parameters headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted.")
      authorizations "oauth2"
      responseMessages response500)

    private val getAllLevelsForLanguage = (apiOperation[Seq[String]]("getAllLevelsForLanguage")
      summary s"Returns all levels for specified language with content in GDL"
      notes s"Returns all levels for specified language with content in GDL"
      parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        pathParam[String]("lang").description("Desired language for books specified in ISO 639-1/2 format."))
      authorizations "oauth2"
      responseMessages response500)

    get("/", operation(getAllLevels)) {
      readService.listAvailablePublishedLevelsForLanguage()
    }

    get("/:lang", operation(getAllLevelsForLanguage)) {
      readService.listAvailablePublishedLevelsForLanguage(Some(LanguageTag(params("lang"))))
    }
  }
}
