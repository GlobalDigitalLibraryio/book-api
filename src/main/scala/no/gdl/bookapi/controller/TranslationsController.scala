/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.{Error, Language, SynchronizeResponse, TranslateRequest, TranslateResponse}
import no.gdl.bookapi.model.domain.TranslationStatus
import no.gdl.bookapi.service.translation.{SupportedLanguageService, TranslationService}
import org.scalatra.{NoContent, NotFound, Ok}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

import scala.util.{Failure, Success}

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

    private val projectFileTranslated = (apiOperation[Unit]("Notify about a file that has been fully translated.")
      summary "Notifies about a file that has been translated fully"
      parameters (
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        queryParam[String]("project").description("Project descriptor for translation system."),
        queryParam[String]("language").description("The language the file has been translated to."),
        queryParam[Long]("file_id").description("The id of the file in the translation system."),
        queryParam[String]("file").description("The name of the file in the translation system.")
      )
      responseMessages(response400, response500))

    get("/supported-languages", operation(getSupportedLanguages)) {
      supportedLanguageService.getSupportedLanguages
    }

    post("/", operation(sendResourceToTranslation)) {
      requireUser
      translationService.addTranslation(extract[TranslateRequest](request.body))
    }

    get("/file-translated", operation(projectFileTranslated)) {
      val projectIdentifier = params("project")
      val crowdinLanguage = params("language")
      val language = LanguageTag(crowdinLanguage)
      val fileId = params("file_id")

      translationService.updateTranslationStatus(projectIdentifier, language, fileId, TranslationStatus.TRANSLATED).flatMap(inTranslationFile =>
        translationService.fetchTranslationsIfAllTranslated(inTranslationFile)) match {
        case Success(_) => NoContent()
        case Failure(err) => errorHandler(err)
      }
    }

    get("/synchronized/:inTranslationId") {
      val inTranslationId = long("inTranslationId")
      logger.info(s"Synchronizing the translation for id $inTranslationId")
      translationService.inTranslationWithId(inTranslationId) match {
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book is currently being translated with inTranslationId $inTranslationId"))
        case Some(inTranslation) => translationService.fetchUpdatesFor(inTranslation)
      }
    }
  }
}
