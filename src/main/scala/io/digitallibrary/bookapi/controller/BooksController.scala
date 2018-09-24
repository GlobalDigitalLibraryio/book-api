/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties.{DefaultLanguage, RoleWithWriteAccess}
import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.model.api.{Chapter, Error, ValidationError}
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.service.search.SearchService
import io.digitallibrary.bookapi.service.translation.MergeService
import io.digitallibrary.bookapi.service.{ContentConverter, ReadService, WriteService}
import io.digitallibrary.language.model.LanguageTag
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait BooksController {
  this: ReadService with WriteService with SearchService with MergeService with ContentConverter =>
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
      description s"Returns a list of books in $DefaultLanguage"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[Int]]("reading-level").description("Return only books matching this reading level."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages response500)

    private val getAllBooksInLang = (apiOperation[api.SearchResult]("getAllBooksInLangDoc")
      summary s"Returns all books in the specified language"
      description s"Returns a list of books in the specified language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[Int]]("reading-level").description("Return only books matching this reading level."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages response500)

    private val getBook = (apiOperation[Option[api.Book]]("getBookInLanguageDoc")
      summary "Returns metadata about a book for the given language"
      description "Returns a book in the given language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book that is to be returned."))
      responseMessages(response400, response404, response500))

    private val updateBook = (apiOperation[Option[api.Book]]("updateBook")
      summary "Updates the metadata of a book."
      description "Updates the metadata of a book."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("The language of the book specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book to be updated."),
      bodyParam[api.Book].description("JSON body"))
      authorizations "oauth2"
      responseMessages(response400, response404, response500))

    private val getChapters = (apiOperation[Seq[api.ChapterSummary]]("getChapters")
      summary "Returns metadata about the chapters for a given book in the given language"
      description "Returns metadata about the chapters for a given book in the given language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book that is to be returned."))
      responseMessages(response400, response404, response500))

    private val getChapter = (apiOperation[Option[api.Chapter]]("getChapter")
      summary "Returns metadata and content for the given chapter in the given language for the given book."
      description "Returns metadata and content for the given chapter in the given language for the given book."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("bookid").description("Id of the book that is to be returned."),
      pathParam[Long]("chapterid").description("Id of the chapter that is to be returned."))
      responseMessages(response400, response404, response500))

    private val updateChapter = (apiOperation[Option[api.Chapter]]("updateChapter")
      summary "Updates the content of a chapter."
      description "Updates the content of a chapter."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("The language of the book specified in ISO 639-2 format"),
      pathParam[Long]("bookid").description("Id of the book to be updated."),
      pathParam[Long]("chapterid").description("Id of the chapter to be updated."),
      bodyParam[api.Chapter].description("JSON body"))
      authorizations "oauth2"
      responseMessages(response400, response404, response500))

    private val getSimilar = (apiOperation[api.SearchResult]("getSimilar")
      summary "Returns books that are similar to the given id in the given language."
      description "Returns books that are similar to the given id in the given language."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book to find similar for."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages(response400, response404, response500))

    private val getMyBooks = (apiOperation[Seq[api.MyBook]]("getMyBooks")
      summary s"Returns all the books for the logged in user."
      description s"Returns a list of books for the logged in user."
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${MyBooksSort.values.mkString(",")}; Default value: ${MyBooksSort.ByIdAsc}"))
      responseMessages (response403, response500)
      authorizations "oauth2")

    private val getFlaggedBooks = (apiOperation[api.SearchResult]("getFlaggedBooks")
      summary s"Returns all flagged books in all languages"
      description s"Returns a list of flagged books"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages response500
      authorizations "oauth2")

    get("/", operation(getAllBooks)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val category = params.get("category")
      val readingLevel = params.get("reading-level")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      val language = LanguageTag(DefaultLanguage)

      searchService.searchWithCategoryAndLevel(
        languageTag = language,
        category = category,
        readingLevel = readingLevel,
        source = None,
        paging = Paging(page, pageSize),
        sort = sort)
    }

    get("/:lang/?", operation(getAllBooksInLang)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val category = params.get("category")
      val readingLevel = params.get("reading-level")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)

      searchService.searchWithCategoryAndLevel(
        languageTag = LanguageTag(params("lang")),
        category = category,
        readingLevel = readingLevel,
        source = None,
        paging = Paging(page, pageSize),
        sort = sort)
    }

    get("/:lang/:id/?", operation(getBook)) {
      val id = long("id")
      val lang = LanguageTag(params("lang"))

      readService.withIdAndLanguageListingAllBooksIfAdmin(id, lang) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $id and language $lang found"))
      }
    }

    put("/:lang/:id/?", operation(updateBook)) {
      assertHasRole(RoleWithWriteAccess)

      val id = long("id")
      val lang = LanguageTag(params("lang"))

      val updatedBook = extract[api.Book](request.body)

      readService.translationWithIdAndLanguageListingAllTranslationsIfAdmin(id, lang) match {
        case Some(existingBook) =>
          val pageOrientationToUpdate = PageOrientation.valueOf(updatedBook.pageOrientation).getOrElse(existingBook.pageOrientation)
          writeService.updateTranslation(existingBook.copy(
            title = updatedBook.title,
            about = updatedBook.description,
            pageOrientation = pageOrientationToUpdate,
            publishingStatus = PublishingStatus.valueOf(updatedBook.publishingStatus).getOrElse(existingBook.publishingStatus),
            additionalInformation = updatedBook.additionalInformation.filter(_.trim.nonEmpty)
        ))
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

    put("/:lang/:bookid/chapters/:chapterid/?", operation(updateChapter)) {

      assertHasRole(RoleWithWriteAccess)

      val lang = LanguageTag(params("lang"))
      val bookId = long("bookid")
      val chapterId = long("chapterid")

      val updatedChapter = extract[Chapter](request.body)

      readService.domainChapterForBookWithLanguageAndId(bookId, lang, chapterId) match {
        case Some(existingChapter) =>
          val chapterTypeToUpdate = ChapterType.valueOf(updatedChapter.chapterType).getOrElse(existingChapter.chapterType)

          val updated = writeService.updateChapter(existingChapter.copy(
            content = mergeService.mergeContents(existingChapter.content, updatedChapter.content),
            chapterType = chapterTypeToUpdate))

          updated.copy(content = contentConverter.toApiContent(updated.content).content)
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
      val sort = MyBooksSort.valueOf(paramOrNone("sort")).getOrElse(MyBooksSort.ByIdAsc)

      readService.forUserWithLanguage(userId, sort)

    }

    get("/flagged/?", operation(getFlaggedBooks)) {
      assertHasRole(RoleWithWriteAccess)

      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)

      readService.withLanguageAndStatus(None, PublishingStatus.FLAGGED, pageSize, page, sort)
    }
  }

}
