/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.{RoleWithAdminReadAccess, RoleWithWriteAccess}
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api.{Error, Language, SynchronizeResponse, TranslateRequest, TranslateResponse}
import io.digitallibrary.bookapi.model.domain.{Paging, Sort, TranslationStatus}
import io.digitallibrary.bookapi.service.ReadService
import io.digitallibrary.bookapi.service.translation.{SupportedLanguageService, TranslationService}
import javax.servlet.http.HttpServletRequest
import org.scalatra.{NoContent, NotFound, Ok}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

import scala.util.{Failure, Success, Try}

trait TranslationsController {
  this: SupportedLanguageService with TranslationService with ReadService =>
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

    private val projectFileProofread = (apiOperation[Unit]("Notify about a file that has been fully proofread.")
      summary "Notifies about a file that has been proofread fully"
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[String]("project").description("Project descriptor for translation system."),
      queryParam[String]("language").description("The language the file has been translated to."),
      queryParam[Long]("file_id").description("The id of the file in the translation system."),
      queryParam[String]("file").description("The name of the file in the translation system."))
      responseMessages(response400, response500))

    private val listAllTranslatedBooks = (apiOperation[api.SearchResult]("List all books that have been translated fully through translation system.")
      summary "List all books that have been translated fully through translation system."
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages(response400, response500))

    private val listAllProofreadBooks = (apiOperation[api.SearchResult]("List all books that have been proofread fully through translation system.")
      summary "List all books that have been proofread fully through translation system."
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages(response400, response500))


    get("/supported-languages", operation(getSupportedLanguages)) {
      supportedLanguageService.getSupportedLanguages
    }

    post("/", operation(sendResourceToTranslation)) {
      val userId = requireUser
      translationService.addTranslation(extract[TranslateRequest](request.body), userId)
    }

    get("/synchronized/:inTranslationId") {
      val inTranslationId = long("inTranslationId")
      logger.info(s"Synchronizing the translation for id $inTranslationId")
      translationService.inTranslationWithId(inTranslationId) match {
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book is currently being translated with inTranslationId $inTranslationId"))
        case Some(inTranslation) => translationService.fetchUpdatesFor(inTranslation)
      }
    }

    get("/file-translated", operation(projectFileTranslated)) {
      fetchAndUpdateStatus(TranslationStatus.TRANSLATED)
    }

    get("/file-proofread", operation(projectFileProofread)) {
      fetchAndUpdateStatus(TranslationStatus.PROOFREAD)
    }

    get("/translated", operation(listAllTranslatedBooks)) {
      withTranslationStatus(TranslationStatus.TRANSLATED)
    }

    get("/proofread", operation(listAllProofreadBooks)) {
      withTranslationStatus(TranslationStatus.PROOFREAD)
    }

    private def fetchAndUpdateStatus(status: TranslationStatus.Value)(implicit request: HttpServletRequest) = {
      val projectIdentifier = params("project")
      val crowdinLanguage = params("language")
      val language = LanguageTag(crowdinLanguage)
      val fileId = params("file_id")

      val translatedFileUpdated = translationService.fetchTranslatedFile(projectIdentifier, crowdinLanguage, fileId, status).map(file => {
        if(translationService.allFilesHaveTranslationStatusGreatherOrEqualTo(file, status)) {
          translationService.markTranslationAs(file, status)
        }
      })

      translatedFileUpdated match {
        case Failure(err) => errorHandler(err)
        case Success(_) => NoContent()
      }
    }

    private def withTranslationStatus(status: TranslationStatus.Value)(implicit request: HttpServletRequest) = {
      assertHasRole(RoleWithAdminReadAccess)
      readService.translationsWithTranslationStatus(
        status,
        Paging(
          intOrDefault("page", 1).max(1),
          intOrDefault("page-size", 10).min(100).max(1)
        ),
        Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      )
    }
  }
}
