/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.readingmaterialsapi.controller

import no.gdl.readingmaterialsapi.ReadingMaterialsApiProperties.{DefaultLanguage, DefaultPageSize}
import no.gdl.readingmaterialsapi.model.api
import no.gdl.readingmaterialsapi.model.api.{AccessDeniedException, Error, ValidationError}
import no.gdl.readingmaterialsapi.model.domain.Sort
import no.gdl.readingmaterialsapi.repository.ReadingMaterialsRepository
import no.gdl.readingmaterialsapi.service.ReadService
import io.digitallibrary.network.AuthUser
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait ReadingMaterialsController {
  this: ReadService with ReadingMaterialsRepository =>
  val readingMaterialsController: ReadingMaterialsController

  class ReadingMaterialsController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for grouping content from ndla.no."

    registerModel[api.Error]
    registerModel[ValidationError]

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val filterReadingMaterialsDoc =
      (apiOperation[String]("filterReadingMaterials")
        summary "Returns reading materials matching a filter"
        notes "Returns a list of reading materials"
        parameters(
          queryParam[Option[String]]("filter").description("A comma separated string containing filters"),
          queryParam[Option[String]]("language").description(s"Only return results on the given language. Default is $DefaultLanguage"),
          queryParam[Option[String]]("sort").description(s"Sort results. Valid options are ${Sort.values.mkString(", ")}"),
          queryParam[Option[Int]]("page-size").description("Return this many results per page"),
          queryParam[Option[Int]]("page").description("Return results for this page")
        )
        authorizations "oauth2"
        responseMessages(response500))

    val getReadingMaterialDoc =
      (apiOperation[String]("getReadingMaterial")
        summary "Returns metadata about a reading material"
        notes "Returns a reading material"
        parameters(
          pathParam[Long]("id").description("Id of the reading material that is to be returned"),
          queryParam[Option[String]]("language").description(s"Return the reading material on this language. Default is $DefaultLanguage")
        )
        authorizations "oauth2"
        responseMessages(response400, response404, response500))


    get("/", operation(filterReadingMaterialsDoc)) {
      val filter = paramAsListOfString("filter")
      val language = paramOrDefault("language", DefaultLanguage)
      val sort = Sort.valueOf(paramOrDefault("sort", "")).getOrElse(Sort.ByIdAsc)
      val pageSize = longOrDefault("page-size", DefaultPageSize)
      val page = longOrDefault("page", 1)

      readService.all(language)
    }

    get("/:id", operation(getReadingMaterialDoc)) {
      val id = long("id")
      val language = paramOrDefault("language", DefaultLanguage)

      readService.withId(id, language) match {
        case Some(cover) => cover
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No reading material with id $id found"))
      }
    }

    def assertHasRole(role: String): Unit = {
      if (!AuthUser.hasRole(role))
        throw new AccessDeniedException("User is missing required role to perform this operation")
    }

  }
}
