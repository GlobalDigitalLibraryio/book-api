/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi.controller

import io.digitallibrary.network.AuthUser
import no.gdl.bookapi.BookApiProperties.DefaultLanguage
import no.gdl.bookapi.model.api
import no.gdl.bookapi.model.api.{AccessDeniedException, Error, ValidationError}
import no.gdl.bookapi.service.{ConverterService, ReadService}
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait BooksController {
  this: ReadService with ConverterService =>
  val booksController: BooksController

  class BooksController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for grouping content from ndla.no."

    registerModel[api.Error]
    registerModel[ValidationError]

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val getAllBooksDoc =
      (apiOperation[String]("filterBooks")
        summary s"Returns all books in the default language $DefaultLanguage"
        notes s"Returns a list of books in $DefaultLanguage"
        parameters(
          headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
          queryParam[Option[Int]]("page-size").description("Return this many results per page"),
          queryParam[Option[Int]]("page").description("Return results for this page")
        )
        authorizations "oauth2"
        responseMessages(response500))

    val getBookDoc =
      (apiOperation[String]("getBook")
        summary "Returns metadata about a book"
        notes "Returns a book"
        parameters(
          pathParam[Long]("id").description("Id of the book that is to be returned"),
          queryParam[Option[String]]("language").description(s"Return the book on this language. Default is $DefaultLanguage")
        )
        authorizations "oauth2"
        responseMessages(response400, response404, response500))

    val getBookInLanguageDoc =
      (apiOperation[String]("getBook")
        summary "Returns metadata about a book for the given language"
        notes "Returns a book in the given language"
        parameters(
          pathParam[Long]("id").description("Id of the book that is to be returned"),
          pathParam[String]("language").description("ISO 639-2 language code")
        )
        authorizations "oauth2"
        responseMessages(response400, response404, response500))


    get("/", operation(getAllBooksDoc)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(0)

      readService.withLanguage(DefaultLanguage, pageSize, page)
    }

    get("/:lang/?" ) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(0)

      readService.withLanguage(params("lang"), pageSize, page)
    }

    get("/:lang/:id/?", operation(getBookInLanguageDoc)) {
      val id = long("id")
      val language = params("lang")

      readService.withIdAndLanguage(id, language) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $id and language $language found"))
      }
    }

    get("/:lang/:id/chapters/?") {
      val language = params("lang")
      val id = long("id")

      readService.chaptersForIdAndLanguage(id, language)
    }

    get("/:lang/:bookid/chapters/:chapterid/?") {
      val language = params("lang")
      val bookId = long("bookid")
      val chapterId = long("chapterid")

      readService.chapterForBookWithLanguageAndId(bookId, language, chapterId) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No chapter with id $chapterId for book with id $bookId and language $language found."))
      }

    }

    get("/:lang/similar/:id/?") {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(0)

      readService.withLanguage(params("lang"), pageSize, page)
    }

    def assertHasRole(role: String): Unit = {
      if (!AuthUser.hasRole(role))
        throw new AccessDeniedException("User is missing required role to perform this operation")
    }
  }
}
