package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties.{DefaultLanguage, RoleWithAdminReadAccess, RoleWithWriteAccess}
import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.model.api.{Chapter, Error, ValidationError}
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.service.search.SearchServiceV2
import io.digitallibrary.bookapi.service.translation.MergeService
import io.digitallibrary.bookapi.service.{ContentConverter, ReadServiceV2, WriteServiceV2}
import io.digitallibrary.language.model.LanguageTag
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

import scala.util.{Failure, Success}


trait BooksControllerV2 {
  this: ReadServiceV2 with WriteServiceV2 with SearchServiceV2 with MergeService with ContentConverter =>
  val booksControllerV2: BooksControllerV2

  class BooksControllerV2(implicit val swagger: Swagger) extends GdlController with SwaggerSupport with CorsSupport {
    protected val applicationDescription = "V2 API for getting books from GDL."

    registerModel[api.Error]
    registerModel[ValidationError]

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getAllBooksV2 = (apiOperation[api.SearchResultV2]("getAllBooksV2")
      summary s"Returns all books in the default language $DefaultLanguage"
      description s"Returns a list of books in $DefaultLanguage"
      tags "Books v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[Int]]("reading-level").description("Return only books matching this reading level."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages response500)

    private val getAllBooksInLangV2 = (apiOperation[api.SearchResultV2]("getAllBooksInLangDocV2")
      summary s"Returns all books in the specified language"
      description s"Returns a list of books in the specified language"
      tags "Books v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[Int]]("reading-level").description("Return only books matching this reading level."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages response500)

    private val getBookV2 = (apiOperation[Option[api.BookV2]]("getBookInLanguageDocV2")
      summary "Returns metadata about a book for the given language"
      description "Returns a book in the given language"
      tags "Books v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book that is to be returned."))
      responseMessages(response400, response404, response500))

    private val updateBookV2 = (apiOperation[Option[api.BookV2]]("updateBookV2")
      summary "Updates the metadata of a book."
      description "Updates the metadata of a book."
      tags "Books v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("The language of the book specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book to be updated."),
      bodyParam[api.Book].description("JSON body"))
      authorizations "oauth2"
      responseMessages(response400, response404, response500))

    private val getChaptersV2 = (apiOperation[Seq[api.ChapterSummary]]("getChaptersV2")
      summary "Returns metadata about the chapters for a given book in the given language"
      description "Returns metadata about the chapters for a given book in the given language"
      tags "Chapters v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book that is to be returned."))
      responseMessages(response400, response404, response500))

    private val getChapterV2 = (apiOperation[Option[api.ChapterV2]]("getChapterV2")
      summary "Returns metadata and content for the given chapter in the given language for the given book."
      description "Returns metadata and content for the given chapter in the given language for the given book."
      tags "Chapters v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("bookid").description("Id of the book that is to be returned."),
      pathParam[Long]("chapterid").description("Id of the chapter that is to be returned."))
      responseMessages(response400, response404, response500))

    private val updateChapterV2 = (apiOperation[Option[api.ChapterV2]]("updateChapterV2")
      summary "Updates the content of a chapter."
      description "Updates the content of a chapter."
      tags "Chapters v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("The language of the book specified in ISO 639-2 format"),
      pathParam[Long]("bookid").description("Id of the book to be updated."),
      pathParam[Long]("chapterid").description("Id of the chapter to be updated."),
      bodyParam[api.Chapter].description("JSON body"))
      authorizations "oauth2"
      responseMessages(response400, response404, response500))

    private val getSimilarV2 = (apiOperation[api.SearchResultV2]("getSimilarV2")
      summary "Returns books that are similar to the given id in the given language."
      description "Returns books that are similar to the given id in the given language."
      tags "Books v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format"),
      pathParam[Long]("id").description("Id of the book to find similar for."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages(response400, response404, response500))

    private val getMyBooksV2 = (apiOperation[Seq[api.MyBookV2]]("getMyBooksV2")
      summary s"Returns all the books for the logged in user."
      description s"Returns a list of books for the logged in user."
      tags "Books v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${MyBooksSort.values.mkString(",")}; Default value: ${MyBooksSort.ByIdAsc}"))
      responseMessages (response403, response500)
      authorizations "oauth2")

    private val getFlaggedBooksV2 = (apiOperation[api.SearchResultV2]("getFlaggedBooksV2")
      summary s"Returns all flagged books in all languages"
      description s"Returns a list of flagged books"
      tags "Books v2"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByIdAsc}"))
      responseMessages response500
      authorizations "oauth2")

    get("/", operation(getAllBooksV2)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val category = params.get("category")
      val readingLevel = params.get("reading-level")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      val language = LanguageTag(DefaultLanguage)

      val result = searchServiceV2.searchWithCategoryAndLevel(
        languageTag = language,
        category = category,
        readingLevel = readingLevel,
        source = None,
        paging = Paging(page, pageSize),
        sort = sort)

      result
    }

    get("/:lang/?", operation(getAllBooksInLangV2)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val category = params.get("category")
      val readingLevel = params.get("reading-level")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)

      searchServiceV2.searchWithCategoryAndLevel(
        languageTag = LanguageTag(params("lang")),
        category = category,
        readingLevel = readingLevel,
        source = None,
        paging = Paging(page, pageSize),
        sort = sort)
    }

    get("/:lang/:id/?", operation(getBookV2)) {
      val id = long("id")
      val lang = LanguageTag(params("lang"))

      readServiceV2.withIdAndLanguageListingAllBooksIfAdmin(id, lang) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $id and language $lang found"))
      }
    }

    put("/:lang/:id/?", operation(updateBookV2)) {
      assertHasRole(RoleWithWriteAccess)

      val id = long("id")
      val lang = LanguageTag(params("lang"))

      val updatedBook = extract[api.BookV2](request.body)

      readServiceV2.translationWithIdAndLanguageListingAllTranslationsIfAdmin(id, lang) match {
        case Some(existingBook) =>
          val pageOrientationToUpdate = PageOrientation.valueOf(updatedBook.pageOrientation).getOrElse(existingBook.pageOrientation)
          writeServiceV2.updateTranslation(existingBook.copy(
            title = updatedBook.title,
            about = updatedBook.description,
            pageOrientation = pageOrientationToUpdate,
            publishingStatus = PublishingStatus.valueOf(updatedBook.publishingStatus).getOrElse(existingBook.publishingStatus),
            additionalInformation = updatedBook.additionalInformation.filter(_.trim.nonEmpty),
            revision = Some(updatedBook.revision.toInt)
          )) match {
            case Success(updated) => readServiceV2.withIdAndLanguage(updated.id.get, updated.language)
            case Failure(failure) => errorHandler(failure)
          }
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $id and language $lang found"))
      }

    }

    get("/:lang/:id/chapters/?", operation(getChaptersV2)) {
      readServiceV2.chaptersForIdAndLanguage(long("id"), LanguageTag(params("lang")))
    }

    get("/:lang/:bookid/chapters/:chapterid/?", operation(getChapterV2)) {
      val lang = LanguageTag(params("lang"))
      val bookId = long("bookid")
      val chapterId = long("chapterid")

      readServiceV2.chapterForBookWithLanguageAndId(bookId, lang, chapterId) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No chapter with id $chapterId for book with id $bookId and language $lang found."))
      }
    }

    put("/:lang/:bookid/chapters/:chapterid/?", operation(updateChapterV2)) {

      assertHasRole(RoleWithWriteAccess)

      val lang = LanguageTag(params("lang"))
      val bookId = long("bookid")
      val chapterId = long("chapterid")

      val updatedChapter = extract[Chapter](request.body)

      readServiceV2.domainChapterForBookWithLanguageAndId(bookId, lang, chapterId) match {
        case Some(existingChapter) =>
          val chapterTypeToUpdate = ChapterType.valueOf(updatedChapter.chapterType).getOrElse(existingChapter.chapterType)

          val updated = writeServiceV2.updateChapter(existingChapter.copy(
            content = mergeService.mergeContents(existingChapter.content, updatedChapter.content),
            chapterType = chapterTypeToUpdate))

          updated.copy(content = contentConverter.toApiContent(updated.content).content)
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No chapter with id $chapterId for book with id $bookId and language $lang found."))
      }
    }

    get("/:lang/similar/:id/?", operation(getSimilarV2)) {
      searchServiceV2.searchSimilar(
        LanguageTag(params("lang")),
        long("id"),
        Paging(
          intOrDefault("page", 1).max(1),
          intOrDefault("page-size", 10).min(100).max(1)
        ),
        Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      )
    }

    get("/mine/?", operation(getMyBooksV2)) {
      val userId = requireUser
      val sort = MyBooksSort.valueOf(paramOrNone("sort")).getOrElse(MyBooksSort.ByIdAsc)

      readServiceV2.forUserWithLanguage(userId, sort)

    }

    get("/flagged/?", operation(getFlaggedBooksV2)) {
      assertHasRole(RoleWithAdminReadAccess)

      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)

      readServiceV2.withLanguageAndStatus(None, PublishingStatus.FLAGGED, pageSize, page, sort)
    }

  }
}
