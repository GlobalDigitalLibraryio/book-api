/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import java.io.OutputStream

import com.fasterxml.jackson.dataformat.csv.{CsvFactory, CsvGenerator, CsvSchema}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.model.api.BookHit
import io.digitallibrary.bookapi.model.domain.{CsvFormat, Paging, Sort}
import io.digitallibrary.bookapi.service.search.SearchService
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model._

trait ExportService {
  this: SearchService with ReadService =>

  val exportService: ExportService

  class ExportService extends LazyLogging {

    val qaSchema: CsvSchema = CsvSchema.builder()
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

    val googlePlaySchema: CsvSchema = CsvSchema.builder()
      .addColumn("Identifier")
      .addColumn("Enable for Sale?")
      .addColumn("Title")
      .addColumn("Subtitle")
      .addColumn("Book Format")
      .addColumn("Related Identifier [Format, Relationship], Semicolon-Separated")
      .addColumn("Contributor [Role], Semicolon-Separated")
      .addColumn("Biographical Note")
      .addColumn("Language")
      .addColumn("Subject Code [Schema], Semicolon-Separated")
      .addColumn("Age Group, Comma-Separated")
      .addColumn("Description")
      .addColumn("Publication Date")
      .addColumn("Page Count")
      .addColumn("Series Name")
      .addColumn("Volume in Series")
      .addColumn("Preview Type")
      .addColumn("Preview Territories")
      .addColumn("Buy Link Text")
      .addColumn("Buy Link")
      .addColumn("Publisher Name")
      .addColumn("Publisher Website")
      .addColumn("Show Photos in Preview?")
      .addColumn("PDF Download Allowed?")
      .addColumn("On Sale Date")
      .addColumn("DRM Enabled?")
      .addColumn("Show Photos in eBook?")
      .addColumn("Include Scanned Pages?")
      .addColumn("For Mature Audiences?")
      .addColumn("Copy-Paste Percentage")
      .setColumnSeparator('\t')
      .build().withHeader()

    val formatToSchema = Map(
      CsvFormat.QualityAssurance -> qaSchema,
      CsvFormat.GooglePlay -> googlePlaySchema
    )

    private val formatToGenerator: Map[CsvFormat.Value, (CsvGenerator, BookHit) => Unit] = Map(
      CsvFormat.QualityAssurance -> writeQualityAssurance,
      CsvFormat.GooglePlay -> writeGooglePlay
    )

    val pageSize = 100
    val baseUrl: String = s"https://${BookApiProperties.Environment}.digitallibrary.io".replace("prod.", "")

    def exportBooks(csvFormat: CsvFormat.Value, language: LanguageTag, source: Option[String], outputStream: OutputStream): Unit = {
      val firstPage = searchService.searchWithQuery(language, None, source, Paging(1, pageSize), Sort.ByIdAsc)

      val numberOfPages = (firstPage.totalCount / pageSize).toInt
      val books = firstPage.results ++
        (1 to numberOfPages).flatMap(i =>
          searchService.searchWithQuery(language, None, source, Paging(i + 1, pageSize), Sort.ByIdAsc).results)

      val factory = new CsvFactory()
      val generator: CsvGenerator = factory.createGenerator(outputStream)
      generator.setSchema(formatToSchema.getOrElse(csvFormat, qaSchema))

      books.foreach(book => formatToGenerator.getOrElse(csvFormat, writeQualityAssurance _)(generator, book))

      generator.writeStartArray()
      generator.writeEndArray()

      generator.flush()
      generator.close()
    }

    def writeQualityAssurance(generator: CsvGenerator, book: BookHit): Unit = {
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
    }

    def writeGooglePlay(generator: CsvGenerator, bookHit: BookHit): Unit = {
      val languageTag = LanguageTag(bookHit.language.code)
      readService.withIdAndLanguageForExport(bookHit.id, languageTag).foreach(book => {
        generator.writeStartArray()
        generator.writeRawValue(book.uuid) // Identifier
        generator.writeString("Yes") // Enable for sale?
        generator.writeString(book.title) // Title
        generator.writeString("") // Subtitle
        generator.writeString("Digital") // Book format
        generator.writeString("") // Related identifier
        generator.writeString(asGooglePlayContributors(book.contributors).mkString(";")) // Contributor
        generator.writeString("") // Biographical note
        generator.writeString(languageTag.language.part1.getOrElse(languageTag.language.id)) // Language
        generator.writeString("") // Subject code
        generator.writeString("") // Age-group
        generator.writeString(book.description)
        generator.writeString("") // Publication date
        generator.writeString("") // Page count
        generator.writeString("") // Series name
        generator.writeString("") // Volume in series
        generator.writeString("20%") // Preview type
        generator.writeString("WORLD") // Preview territory
        generator.writeString("") // Buy link text
        generator.writeString("") // Buy link
        generator.writeString("Global Digital Library") // Publisher name
        generator.writeString("https://www.digitallibrary.io/") // Publisher website
        generator.writeString("Yes") // Show pictures in Preview
        generator.writeString("No") // PDF Download allowed
        generator.writeString("") // On sale date
        generator.writeString("No") // DRM Enabled?
        generator.writeString("Yes") // Show photos in eBook?
        generator.writeString("Yes") // Include scanned pages?
        generator.writeString("No") // For mature audience?
        generator.writeString("0%") // Copy paste percentage

        generator.writeEndArray()
      })
    }

    def asGooglePlayContributors(contributors: Seq[api.Contributor]): Seq[String] = {
      contributors.map(c => s"${c.name} [${c.`type`}]")
    }
  }
}
