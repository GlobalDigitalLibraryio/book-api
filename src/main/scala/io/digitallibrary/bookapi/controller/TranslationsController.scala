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
import io.digitallibrary.bookapi.model.api.{BookForTranslation, CrowdinException, Error, Language, SynchronizeResponse, TranslateRequest, TranslateResponse}
import io.digitallibrary.bookapi.model.domain.{Paging, Sort, TranslationStatus}
import io.digitallibrary.bookapi.service.{ConverterService, ReadService}
import io.digitallibrary.bookapi.service.translation.{SupportedLanguageService, SynchronizeService, TranslationService}
import javax.servlet.http.HttpServletRequest
import org.postgresql.util.PSQLException
import org.scalatra.{InternalServerError, NoContent, NotFound, Ok}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

import scala.util.{Failure, Success, Try}

trait TranslationsController {
  this: SupportedLanguageService with TranslationService with SynchronizeService with ReadService with ConverterService =>
  val translationsController: TranslationsController

  class TranslationsController (implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for retrieving all languages from the GDL"

    registerModel[api.Error]
    registerModel[api.ValidationError]

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response404 = ResponseMessage(404, "Not Found", Some("Not Found"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getSupportedLanguages = (apiOperation[Seq[Language]]("List supported languages")
      summary "Retrieves a list of all supported languages to translate to"
      tags "Translations v1"
      parameters
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted.")
      responseMessages(response400, response500))

    private val getTranslationProjects = (apiOperation[Map[String, String]]("Map language codes to translation projects")
      summary "Retrieves all translation projects"
      tags "Translations v1"
      parameters
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted.")
      responseMessages(response400, response500))

    private val getSupportedLanguagesForLanguage = (apiOperation[Seq[Language]]("List supported languages")
      summary "Retrieves a list of all supported languages to translate to when translating from given language"
      tags "Translations v1"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("language").description("The language to translate from."))
      responseMessages(response400, response500))

    private val getBookForTranslation = (apiOperation[BookForTranslation]("Get book for translation")
      summary "Retrieves the metadata for the book for translation"
      tags "Translations v1"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("language").description("The language the book is being translated from"),
      pathParam[String]("book_id").description("The id of the book to translate"))
      responseMessages(response400, response404, response500))

    private val getChapterForTranslation = (apiOperation[api.Chapter]("List supported languages")
    summary "Retrieves the chapter for translation"
    tags "Translations v1"
    parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("language").description("The language the book is being translated from"),
      pathParam[String]("book_id").description("The id of the book to translate"),
      pathParam[String]("chapter_id").description("The id of the chapter to translate"))
      responseMessages(response400, response404, response500))

    private val sendResourceToTranslation = (apiOperation[TranslateResponse]("Send book to translation")
      summary "Sends a book to translation system"
      tags "Translations v1"
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      bodyParam[TranslateRequest])
      responseMessages(response400, response500)
      authorizations "oauth2")

    private val projectFileTranslated = (apiOperation[Unit]("Notify about a file that has been fully translated.")
      summary "Notifies about a file that has been translated fully"
      tags "Translations v1"
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
      tags "Translations v1"
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[String]("project").description("Project descriptor for translation system."),
      queryParam[String]("language").description("The language the file has been translated to."),
      queryParam[Long]("file_id").description("The id of the file in the translation system."),
      queryParam[String]("file").description("The name of the file in the translation system."))
      responseMessages(response400, response500))

    private val listAllTranslatedBooks = (apiOperation[api.SearchResult]("List all books that have been translated fully through translation system.")
      summary "List all books that have been translated fully through translation system."
      tags "Translations v1"
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages(response400, response500)
      authorizations "oauth2")

    private val listAllProofreadBooks = (apiOperation[api.SearchResult]("List all books that have been proofread fully through translation system.")
      summary "List all books that have been proofread fully through translation system."
      tags "Translations v1"
      parameters (
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages(response400, response500)
      authorizations "oauth2")

    get("/:language/:book_id/?", operation(getBookForTranslation)) {
      val translatedFrom = LanguageTag(params("language"))
      val bookId = long("book_id")
      translationService.forTranslation(translatedFrom, bookId) match {
        case Some(x) => Success(x)
        case None => NotFound(s"Book with id $bookId not possible to translate")
      }
    }

    get("/:language/:book_id/chapters/:chapter_id/?", operation(getChapterForTranslation)) {
      val translatedFrom = LanguageTag(params("language"))
      val bookId = long("book_id")
      val chapterId = long("chapter_id")
      translationService.forTranslationAndChapter(bookId, chapterId) match {
        case Some(x) => Success(x)
        case None => NotFound(s"Chapter with id $chapterId for book $bookId not possible to translate")
      }
    }

    get("/supported-languages", operation(getSupportedLanguages)) {
      supportedLanguageService.getSupportedLanguages()
    }

    get("/translation-projects", operation(getTranslationProjects)) {
      BookApiProperties.CrowdinProjects.map(project => (LanguageTag(project.sourceLanguage).toString, project.projectIdentifier)).toMap
    }

    get("/:language/supported-languages", operation(getSupportedLanguagesForLanguage)) {
      val crowdinLanguage = params("language")
      val language = LanguageTag(crowdinLanguage)
      supportedLanguageService.getSupportedLanguages(Some(language))
    }

    post("/", operation(sendResourceToTranslation)) {
      val userId = requireUser
      val translateRequest = extract[TranslateRequest](request.body)
      translationService.addTranslation(converterService.asDomainTranslateRequest(translateRequest, userId))
    }

    get("/synchronized/:inTranslationId") {
      val inTranslationId = long("inTranslationId")
      logger.info(s"Synchronizing the translation for id $inTranslationId")
      translationService.inTranslationWithId(inTranslationId) match {
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book is currently being translated with inTranslationId $inTranslationId"))
        case Some(inTranslation) => {
          synchronizeService.fetchPseudoFiles(inTranslation)
          synchronizeService.fetchUpdatesFor(inTranslation)
        }
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

      val translatedFileUpdated = synchronizeService.fetchTranslatedFile(projectIdentifier, crowdinLanguage, fileId, status).map(file => {
        if(synchronizeService.allFilesHaveTranslationStatusGreatherOrEqualTo(file, status)) {
          synchronizeService.markTranslationAs(file, status)
        }
      })

      translatedFileUpdated match {
        case Success(_) => NoContent()
        case Failure(c: CrowdinException) =>
          logger.error(c.getMessage, c)
          c.getErrors.foreach(error => logger.error(s"${c.getMessage}: ${error.code} - ${error.message}"))
          c.getCauses.foreach(throwable => logger.error(c.getMessage, throwable))
          InternalServerError(body = Error.TranslationError)
        case Failure(p: PSQLException) if p.getMessage.contains("translation_uniq_book_id_language") =>
          logger.error(Error.GenericError.toString, p)
          InternalServerError(body = Error.GenericError)
        case Failure(err) =>
          logger.error(Error.GenericError.toString, err)
          NoContent()
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
