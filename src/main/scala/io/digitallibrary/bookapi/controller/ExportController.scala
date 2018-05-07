/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.RoleWithWriteAccess
import io.digitallibrary.bookapi.service.ExportService
import io.digitallibrary.language.model.LanguageTag
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}


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
      description s"Returns a csv with books for langage and source"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Export books for language."),
      pathParam[String]("source").description(s"Exports books for source. 'all' gives all sources"))
      authorizations "oauth2"
      responseMessages response500)

    get("/:lang/:source", operation(exportBooks)) {
      assertHasRole(RoleWithWriteAccess)

      val language = LanguageTag(params("lang"))
      val source = params("source")
      val searchSource = if (source.equals("all")) None else Some(source)

      contentType = "text/csv"
      response.setHeader("Content-Disposition", s"""attachment; filename="books-$language-$source.csv" """)

      exportService.exportBooks(language, searchSource, response.getOutputStream)
    }
  }

}
