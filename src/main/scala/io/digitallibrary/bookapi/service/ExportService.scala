/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import java.io.OutputStream

import com.fasterxml.jackson.dataformat.csv.{CsvFactory, CsvSchema}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.model.domain.{Paging, Sort}
import io.digitallibrary.bookapi.service.search.SearchService
import io.digitallibrary.language.model.LanguageTag

trait ExportService {
  this: SearchService =>

  val exportService: ExportService

  class ExportService extends LazyLogging {

    val schema: CsvSchema = CsvSchema.builder()
      .addColumn("id")
      .addColumn("environment")
      .addColumn("language")
      .addColumn("title")
      .addColumn("description")
      .addColumn("source")
      .addColumn("url")
      .addColumn("approved")
      .addColumn("comment")
      .build().withHeader()
    val pageSize = 100
    val baseUrl: String = s"https://${BookApiProperties.Environment}.digitallibrary.io".replace("prod.", "")

    def exportBooks(language: LanguageTag, source: Option[String], outputStream: OutputStream): Unit = {
      val firstPage = searchService.searchWithQuery(language, None, source, Paging(1, pageSize), Sort.ByIdAsc)

      val numberOfPages = (firstPage.totalCount / pageSize).toInt
      val books = firstPage.results ++
        (1 to numberOfPages).flatMap(i =>
          searchService.searchWithQuery(language, None, source, Paging(i + 1, pageSize), Sort.ByIdAsc).results)

      val factory = new CsvFactory()
      val generator = factory.createGenerator(outputStream)
      generator.setSchema(schema)

      books.foreach(book => {
        generator.writeStartArray()
        generator.writeRawValue(book.id.toString)
        generator.writeRawValue(BookApiProperties.Environment)
        generator.writeRawValue(language.toString)
        generator.writeString(book.title)
        generator.writeString(book.description)
        generator.writeRawValue(book.source)
        generator.writeRawValue(s"$baseUrl/${language.toString}/books/details/${book.id}")
        generator.writeRawValue("")
        generator.writeRawValue("")
        generator.writeEndArray()
      })

      generator.writeStartArray()
      generator.writeEndArray()

      generator.flush()
      generator.close()
    }
  }
}
