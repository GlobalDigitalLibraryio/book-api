/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.BookApiProperties.DefaultLanguage
import no.gdl.bookapi.model.api
import no.gdl.bookapi.model.api.ValidationError
import no.gdl.bookapi.model.domain.{Paging, Sort}
import no.gdl.bookapi.service.ConverterService
import no.gdl.bookapi.service.search.SearchService
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait SearchController {
  this: SearchService with ConverterService =>
  val searchController: SearchController

  class SearchController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for searching books from GDL."

    def extractPageAndPageSize(): Paging = {
      Paging(
        page = intOrDefault("page", 1).max(1),
        pageSize = intOrDefault("page-size", BookApiProperties.DefaultPageSize).min(BookApiProperties.MaxPageSize).max(1)
      )
    }

    registerModel[api.Error]
    registerModel[ValidationError]

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val searchBooks = (apiOperation[api.SearchResult]("searchBooks")
      summary s"Search for books in the default language $DefaultLanguage"
      notes s"Returns a list of books in $DefaultLanguage"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("query").description("Query to search for"))
      responseMessages response500)

    private val searchBooksForLang = (apiOperation[api.SearchResult]("searchBooks")
      summary "Search for books in the provided language"
      notes "Returns a list of books in the provided language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in BCP-47 format."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("query").description("Query to search for"))
      responseMessages response500)

    get("/", operation(searchBooks)) {
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      val language = LanguageTag(DefaultLanguage)
      val query = paramOrNone("query")

      searchService.searchBook(query = query, language = language, paging = extractPageAndPageSize())
    }

    get("/:lang/?", operation(searchBooksForLang)) {
      val readingLevel = params.get("reading-level")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      val query = paramOrNone("query")

      searchService.searchBook(query = query, LanguageTag(params("lang")), paging = extractPageAndPageSize())
    }
  }
}
