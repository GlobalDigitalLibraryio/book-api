/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.RoleWithAdminReadAccess
import io.digitallibrary.bookapi.model.domain.CsvFormat
import io.digitallibrary.bookapi.service.ExportService
import io.digitallibrary.language.model.LanguageTag
import javax.servlet.http.HttpServletRequest
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra.util.UrlCodingUtils


trait ExportController {
  this: ExportService =>
  val exportController: ExportController

  class ExportController(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for exporting books from GDL."

    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))
    val baseUrl = s"https://${if(BookApiProperties.Environment.equals("prod")) "" else BookApiProperties.Environment + "."}digitallibrary.io"

    private val exportBooks = (apiOperation[Array[Byte]]("exportBooks")
      produces "text/csv"
      summary s"Export all books for language and source"
      description s"Returns a csv with books for language and source"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Export books for language specified in BCP-47 format."),
      pathParam[String]("source").description(s"Exports books for source. 'all' gives all sources"))
      authorizations "oauth2"
      responseMessages response500)

    private val exportAsGooglePlayCsv = (apiOperation[Array[Byte]]("exportasGooglePlayCsv")
      produces "text/csv"
      summary s"Export all books for language and source on format for google play"
      description s"Returns a csv with books for language and source formatted for google play"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Export books for language specified in BCP-47 format."),
      pathParam[String]("source").description(s"Exports books for source. 'all' gives all sources"))
      authorizations "oauth2"
      responseMessages response500)

    private val exportEpubsAsZipFile = (apiOperation[Array[Byte]]("exportEpubsAsZipFile")
      produces "application/octet-stream"
      summary s"Export all books as epub files for language and source"
      description s"Export all books as epub files for language and source"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Export books for language specified in BCP-47 format."),
      pathParam[String]("source").description(s"Exports books for source. 'all' gives all sources"))
      authorizations "oauth2"
      responseMessages response500)

    get("/:lang/:source", operation(exportBooks)) {
      exportBooks(CsvFormat.QualityAssurance, "qualityAssurance")
    }

    get("/googleplay/:lang/:source", operation(exportAsGooglePlayCsv)) {
      exportBooks(CsvFormat.GooglePlay, "googlePlay")
    }

    get("/epubs/:lang/:source", operation(exportEpubsAsZipFile)) {
      assertHasRole(RoleWithAdminReadAccess)

      val language = LanguageTag(params("lang"))
      val source = params("source")
      val searchSource = if (source.equals("all")) None else Some(source)

      contentType = "application/octet-stream"
      response.setHeader("Content-Disposition", s"""attachment; filename="books-${BookApiProperties.Environment}-$language-$source.zip" """)
      exportService.getAllEPubsAsZipFile(language, searchSource, response.getOutputStream)
    }

    private def exportBooks(format: CsvFormat.Value, fileEnding: String)(implicit request: HttpServletRequest): Any = {
      assertHasRole(RoleWithAdminReadAccess)

      val language = LanguageTag(params("lang"))
      val source = params("source")
      val searchSource = if (source.equals("all")) None else Some(source)

      contentType = "text/csv"
      response.setHeader("Content-Disposition", s"""attachment; filename="books-${BookApiProperties.Environment}-$language-$source-$fileEnding.csv" """)

      exportService.exportBooks(format, language, searchSource, response.getOutputStream)
    }
  }
}
