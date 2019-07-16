/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties.{DefaultLanguage, FeaturedContentAdminRole}
import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.model.api.{Error, FeaturedContent, FeaturedContentId, ValidationException}
import io.digitallibrary.bookapi.service.{ReadService, WriteService}
import io.digitallibrary.language.model.LanguageTag
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra.{Created, NotAcceptable, NotFound, Ok}

import scala.util.{Failure, Success}


trait FeaturedContentController {
  this: ReadService with WriteService =>
  val featuredContentController: FeaturedContentController

  class FeaturedContentController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {

    registerModel[api.Error]

    protected val applicationDescription = "API for retrieving featured content from GDL"

    private val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))

    private val correlationIDParameter = headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted.")

    private val getAllFeaturedContentInDefaultLang = (apiOperation[Seq[api.FeaturedContent]]("getAllFeaturedContentInDefaultLang")
      summary "Returns all featured content in default language"
      tags "Featured v1"
      notes "Returns all featured content in default language"
      parameters correlationIDParameter
      responseMessages response500)

    private val getAllFeaturedContentInLang = (apiOperation[Seq[api.FeaturedContent]]("getAllFeaturedContentInLang")
      summary "Returns all featured content in given language"
      tags "Featured v1"
      notes "Returns all featured content in given language"
      parameters(correlationIDParameter,
      pathParam[String]("lang").description("Desired language for featured content specified in ISO 639-1/2 format."))
      responseMessages response500)

    private val addNewFeaturedContent = (apiOperation[FeaturedContentId]("addNewFeaturedContent")
      summary "Adds a new featured content"
      tags "Featured v1"
      notes "Returns id of created featured content"
      parameters(correlationIDParameter, bodyParam[NewFeaturedContent]("JSON body"))
      responseMessages response500
      authorizations "oauth2")

    private val updateExistingFeaturedContent = (apiOperation[FeaturedContentId]("updateExistingFeaturedContent")
      summary "Updates an existing featured content"
      tags "Featured v1"
      notes "Returns id of updated featured content"
      parameters(correlationIDParameter, bodyParam[FeaturedContent]("JSON body"))
      responseMessages response500
      authorizations "oauth2")

    private val deleteFeaturedContent = (apiOperation[FeaturedContentId]("deleteFeaturedContent")
      summary "Deletes an existing featured content"
      tags "Featured v1"
      notes "Returns 200 OK if featured content was deleted"
      parameters(correlationIDParameter, pathParam[Long]("id").description("ID of featured content to delete"))
      responseMessages response500
      authorizations "oauth2")

    get("/", operation(getAllFeaturedContentInDefaultLang)) {
      readService.featuredContentForLanguage(LanguageTag(DefaultLanguage))
    }

    get("/:lang", operation(getAllFeaturedContentInLang)) {
      val contentForLang = readService.featuredContentForLanguage(LanguageTag(params("lang")))
      if (contentForLang.nonEmpty) contentForLang else readService.featuredContentForLanguage(LanguageTag(DefaultLanguage))
    }

    post("/", operation(addNewFeaturedContent)) {
      assertHasRole(FeaturedContentAdminRole)
      val newFeaturedContent = extract[NewFeaturedContent](request.body)
      writeService.newFeaturedContent(newFeaturedContent) match {
        case Success(x) => Created(x)
        case Failure(ex: ValidationException) => NotAcceptable(body = Error(Error.VALIDATION, ex.errors.map(_.message).mkString(", ")))
        case Failure(ex) => throw ex
      }
    }

    put("/", operation(updateExistingFeaturedContent)) {
      assertHasRole(FeaturedContentAdminRole)
      val updatedContent = extract[FeaturedContent](request.body)
      writeService.updateFeaturedContent(updatedContent) match {
        case Success(x) => Ok(x)
        case Failure(ex: ValidationException) => NotAcceptable(body = Error(Error.VALIDATION, ex.errors.map(_.message).mkString(", ")))
        case Failure(ex) => throw ex
      }
    }

    delete("/:id", operation(deleteFeaturedContent)) {
      assertHasRole(FeaturedContentAdminRole)
      val id = long("id")
      contentType = formats("txt")
      writeService.deleteFeaturedContent(id) match {
        case Success(_) => Ok(s"Deleted $id")
        case Failure(_) => NotFound(s"No featured content with id=$id found")
      }
    }

  }

}
