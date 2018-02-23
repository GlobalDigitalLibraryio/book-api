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
import no.gdl.bookapi.model.api.{Book, Error, ValidationError}
import no.gdl.bookapi.model.domain.{Paging, Sort}
import no.gdl.bookapi.service.search.SearchService
import no.gdl.bookapi.service.{ConverterService, ReadService}
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait BooksController {
  this: ReadService with ConverterService with SearchService =>
  val booksController: BooksController

  class BooksController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for getting books from GDL."

    registerModel[api.Error]
    registerModel[ValidationError]

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getAllBooks = (apiOperation[api.SearchResult]("getAllBooks")
      summary s"Returns all books in the default language $DefaultLanguage"
      notes s"Returns a list of books in $DefaultLanguage"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[Int]]("reading-level").description("Return only books matching this reading level."))
      responseMessages response500)

    private val getAllBooksInLang = (apiOperation[api.SearchResult]("getAllBooksInLangDoc")
      summary s"Returns all books in the specified language"
      notes s"Returns a list of books in the specified language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[Int]]("reading-level").description("Return only books matching this reading level."))
      responseMessages response500)

    private val getBook = (apiOperation[Option[api.Book]]("getBookInLanguageDoc")
      summary "Returns metadata about a book for the given language"
      notes "Returns a book in the given language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book that is to be returned."))
      responseMessages(response400, response404, response500))

    private val getChapters = (apiOperation[Seq[api.ChapterSummary]]("getChapters")
      summary "Returns metadata about the chapters for a given book in the given language"
      notes "Returns metadata about the chapters for a given book in the given language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book that is to be returned."))
      responseMessages(response400, response404, response500))

    private val getChapter = (apiOperation[Option[api.Chapter]]("getChapter")
      summary "Returns metadata and content for the given chapter in the given language for the given book."
      notes "Returns metadata and content for the given chapter in the given language for the given book."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("bookid").description("Id of the book that is to be returned."),
      pathParam[Long]("chapterid").description("Id of the chapter that is to be returned."))
      responseMessages(response400, response404, response500))

    private val getSimilar = (apiOperation[api.SearchResult]("getSimilar")
      summary "Returns books that are similar to the given id in the given language."
      notes "Returns books that are similar to the given id in the given language."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book to find similar for."))
      responseMessages(response400, response404, response500))

    private val getMyBooks = (apiOperation[Seq[api.MyBook]]("getMyBooks")
      summary s"Returns all the books for the logged in user."
      notes s"Returns a list of books for the logged in user."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("sort").description("Sorting order for the books."))
      responseMessages (response403, response500)
      authorizations "oauth2")

    get("/", operation(getAllBooks)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val readingLevel = params.get("reading-level")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      val language = LanguageTag(DefaultLanguage)

      searchService.searchWithLevel(language, readingLevel, Paging(page, pageSize), sort)
    }

    get("/:lang/?", operation(getAllBooksInLang)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val readingLevel = params.get("reading-level")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)

      searchService.searchWithLevel(LanguageTag(params("lang")), readingLevel, Paging(page, pageSize), sort)
    }

    get("/:lang/:id/?", operation(getBook)) {
      val id = long("id")
      val lang = LanguageTag(params("lang"))

      readService.withIdAndLanguage(id, lang) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $id and language $lang found"))
      }
    }

    get("/:lang/:id/chapters/?", operation(getChapters)) {
      readService.chaptersForIdAndLanguage(long("id"), LanguageTag(params("lang")))
    }

    get("/:lang/:bookid/chapters/:chapterid/?", operation(getChapter)) {
      val lang = LanguageTag(params("lang"))
      val bookId = long("bookid")
      val chapterId = long("chapterid")

      readService.chapterForBookWithLanguageAndId(bookId, lang, chapterId) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No chapter with id $chapterId for book with id $bookId and language $lang found."))
      }
    }

    get("/:lang/similar/:id/?", operation(getSimilar)) {
      searchService.searchSimilar(
        LanguageTag(params("lang")),
        long("id"),
        Paging(
          intOrDefault("page", 1).max(1),
          intOrDefault("page-size", 10).min(100).max(1)
        ),
        Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      )
    }

    get("/mine/?", operation(getMyBooks)) {
      val userId = requireUser
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)

      readService.forUserWithLanguage(userId, pageSize, page, sort)

    }
  }

}
