package no.gdl.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties.DefaultLanguage
import no.gdl.bookapi.model.api
import no.gdl.bookapi.model.domain.Sort
import no.gdl.bookapi.service.search.SearchService
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
/**
  * @author gv@knowit.no
  */
trait SearchController {
  this: SearchService =>
  val searchController: SearchController

  class SearchController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for searching books from GDL."

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
      summary s"Search for books in the default language $DefaultLanguage"
      notes s"Returns a list of books in $DefaultLanguage"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-2 format."),
      queryParam[Option[Int]]("page-size").description("Return this many results per page."),
      queryParam[Option[Int]]("page").description("Return results for this page."),
      queryParam[Option[String]]("query").description("Query to search for"))
      responseMessages response500)

    get("/", operation(searchBooks)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      val language = LanguageTag(DefaultLanguage)
      val query = paramOrNone("query")

      searchService.search(query = query, language = language, page = page, pageSize = pageSize)
    }

    get("/:lang/?", operation(searchBooksForLang)) {
      val pageSize = intOrDefault("page-size", 10).min(100).max(1)
      val page = intOrDefault("page", 1).max(1)
      val readingLevel = params.get("reading-level")
      val sort = Sort.valueOf(paramOrNone("sort")).getOrElse(Sort.ByIdAsc)
      val query = paramOrNone("query")

      searchService.search(query = query, LanguageTag(params("lang")), page = page, pageSize = pageSize)
    }
  }
}
