/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.DefaultLanguage
import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.model.api.ValidationError
import io.digitallibrary.bookapi.model.domain.{Paging, Sort}
import io.digitallibrary.bookapi.service.ConverterService
import io.digitallibrary.bookapi.service.search.SearchService
import io.digitallibrary.language.model.LanguageTag
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
      description s"Returns a list of books in $DefaultLanguage"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("query").description("Query to search for"),
      queryParam[Option[String]]("source").description("Filter results by source"),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByRelevance}"))
      responseMessages response500)

    private val searchBooksForLang = (apiOperation[api.SearchResult]("searchBooksForLang")
      summary "Search for books in the provided language"
      description "Returns a list of books in the provided language"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in BCP-47 format."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("query").description("Query to search for"),
      queryParam[Option[String]]("source").description("Filter results by source"),
      queryParam[Option[String]]("sort").description(s"Sorts result based on parameter. Possible values: ${Sort.values.mkString(",")}; Default value: ${Sort.ByRelevance}"))
      responseMessages response500)

    get("/", operation(searchBooks)) {
      val query = paramOrNone("query")
      val source = paramOrNone("source")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByRelevance)
      searchService.searchWithQuery(
        languageTag = LanguageTag(DefaultLanguage),
        query = query,
        source = source,
        paging = extractPageAndPageSize(),
        sort = sort)
    }

    get("/:lang/?", operation(searchBooksForLang)) {
      val query = paramOrNone("query")
      val source = paramOrNone("source")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByRelevance)

      searchService.searchWithQuery(
        languageTag = LanguageTag(params("lang")),
        query = query,
        source = source,
        paging = extractPageAndPageSize(),
        sort = sort)
    }
  }
}
