/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.service.ReadService
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties.RoleWithAdminReadAccess
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait SourceController {
  this: ReadService =>
  val sourceController: SourceController

  class SourceController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for getting sources from GDL."

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getSourcesForLanguage = (apiOperation[Seq[String]]("getSourcesForLanguage")
      summary s"Returns all the sources for the specified language"
      description s"Returns a list of sources in the specified language"
      parameter pathParam[String]("lang").description("The language to receive sources for in ISO 639-2 format")
      authorizations "oauth2"
      responseMessages(response400, response403, response404, response500))

    get("/:lang", operation(getSourcesForLanguage)) {
      assertHasRole(RoleWithAdminReadAccess)

      val language = if(params("lang") == "all") None else Some(LanguageTag(params("lang")))
      readService.listSourcesForLanguage(language)
    }

    get("/") {
      redirect(s"${BookApiProperties.SourcePath}/${BookApiProperties.DefaultLanguage}")
    }
  }
}
