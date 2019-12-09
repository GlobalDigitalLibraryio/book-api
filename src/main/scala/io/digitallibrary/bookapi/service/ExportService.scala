/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import java.io.{FileOutputStream, OutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.fasterxml.jackson.dataformat.csv.{CsvFactory, CsvGenerator, CsvSchema}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.{ImageApiClient, MediaApiClient}
import io.digitallibrary.bookapi.model.api.{BookHit, BookHitV2}
import io.digitallibrary.bookapi.model.domain.{CsvFormat, Paging, Sort}
import io.digitallibrary.bookapi.service.search.{SearchService, SearchServiceV2}
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model._
import io.digitallibrary.network.GdlClient
import scalaj.http.{Http, HttpOptions}

import scala.util.{Failure, Success}

trait ExportService {
  this: SearchServiceV2 with ReadServiceV2 with GdlClient with MediaApiClient =>

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
      .addColumn("USD [Recommended Retail Price, Excluding Tax] Price")
      .addColumn("Countries for USD [Recommended Retail Price, Excluding Tax] Price")
      .setColumnSeparator('\t')
      .build().withHeader()

    val formatToSchema = Map(
      CsvFormat.QualityAssurance -> qaSchema,
      CsvFormat.GooglePlay -> googlePlaySchema
    )

    private val formatToGenerator: Map[CsvFormat.Value, (CsvGenerator, BookHitV2) => Unit] = Map(
      CsvFormat.QualityAssurance -> writeQualityAssurance,
      CsvFormat.GooglePlay -> writeGooglePlay
    )

    val pageSize = 100
    val baseUrl: String = s"https://${BookApiProperties.Environment}.digitallibrary.io".replace("prod.", "")

    def getAllEPubsAsZipFile(language: LanguageTag, source: Option[String], outputStream: OutputStream): Unit = {
      val firstPage = searchServiceV2.searchWithQuery(language, None, source, Paging(1, pageSize), Sort.ByIdAsc)

      val numberOfPages = (firstPage.totalCount / pageSize).toInt
      val books = firstPage.results ++
        (1 to numberOfPages).flatMap(i =>
          searchServiceV2.searchWithQuery(language, None, source, Paging(i + 1, pageSize), Sort.ByIdAsc).results)

      val zip = new ZipOutputStream(outputStream)
      books
        .flatMap(b => readServiceV2.withIdAndLanguageForExport(b.id, language))
        .filter(_.downloads.epub.isDefined)
        .foreach(book => {
          val urlToEpub = book.downloads.epub.get
          gdlClient.fetchBytes(Http(urlToEpub).option(HttpOptions.readTimeout(600000))) match {
            case Failure(error) => logger.error(s"Error when producing zip-archive for epubs for $language and $source. Could not download $urlToEpub", error)
            case Success(bytes) =>
              zip.putNextEntry(new ZipEntry(s"PKEY:${book.uuid}.epub"))
              zip.write(bytes)
              zip.closeEntry()
          }

        })
      zip.flush()
      zip.close()
    }

    def getAllCoverImagesAsZipFile(language: LanguageTag, source: Option[String], outputStream: OutputStream): Unit = {
      val firstPage = searchServiceV2.searchWithQuery(language, None, source, Paging(1, pageSize), Sort.ByIdAsc)

      val numberOfPages = (firstPage.totalCount / pageSize).toInt
      val books = firstPage.results ++
        (1 to numberOfPages).flatMap(i =>
          searchServiceV2.searchWithQuery(language, None, source, Paging(i + 1, pageSize), Sort.ByIdAsc).results)

      val zip = new ZipOutputStream(outputStream)
      books
        .filter(_.coverImage.isDefined)
        .flatMap(b => readServiceV2.withIdAndLanguageForExport(b.id, language))
        .foreach(book => {
          mediaApiClient.downloadImage(book.coverPhoto.get.imageApiId, language = language.toString()) match {
            case Failure(error) => logger.error(s"Error when producing zip-archive with cover images for $language and $source. Could not download cover image with id ${book.coverPhoto.get.imageApiId}", error)
            case Success(downloadedImage) =>
              zip.putNextEntry(new ZipEntry(s"PKEY:${book.uuid}_frontcover.${downloadedImage.fileEnding}"))
              zip.write(downloadedImage.bytes)
              zip.closeEntry()

          }
        })
      zip.flush()
      zip.close()
    }

    def exportBooks(csvFormat: CsvFormat.Value, language: LanguageTag, source: Option[String], outputStream: OutputStream): Unit = {
      val firstPage = searchServiceV2.searchWithQuery(language, None, source, Paging(1, pageSize), Sort.ByIdAsc)

      val numberOfPages = (firstPage.totalCount / pageSize).toInt
      val books = firstPage.results ++
        (1 to numberOfPages).flatMap(i =>
          searchServiceV2.searchWithQuery(language, None, source, Paging(i + 1, pageSize), Sort.ByIdAsc).results)

      val factory = new CsvFactory()
      val generator: CsvGenerator = factory.createGenerator(outputStream)
      generator.setSchema(formatToSchema.getOrElse(csvFormat, qaSchema))

      printStaticValues(generator, csvFormat)
      books.foreach(book => formatToGenerator.getOrElse(csvFormat, writeQualityAssurance _)(generator, book))

      generator.writeStartArray()
      generator.writeEndArray()

      generator.flush()
      generator.close()
    }

    def printStaticValues(generator: CsvGenerator, format: CsvFormat.Value): Unit = {
      format match {
        case CsvFormat.GooglePlay => writeGooglePlayReadOnlyRow(generator)
        case _ =>
      }
    }

    def writeQualityAssurance(generator: CsvGenerator, book: BookHitV2): Unit = {
      val languageTag = LanguageTag(book.language.code)
      generator.writeStartArray()
      generator.writeRawValue(book.id.toString)
      generator.writeRawValue(BookApiProperties.Environment)
      generator.writeRawValue(languageTag.toString)
      generator.writeString(book.title)
      generator.writeString(book.description)
      generator.writeRawValue(book.source)
      generator.writeRawValue(s"$baseUrl/${languageTag.toString}/books/details/${book.id}")
      generator.writeRawValue("")
      generator.writeRawValue("")
      generator.writeEndArray()
    }

    def writeGooglePlayReadOnlyRow(generator: CsvGenerator): Unit = {
      generator.writeStartArray()
      generator.writeRawValue("Default values for collection code: {KQ7RRL5}. This row is read-only; do not modify the data. Any cells you leave blank in a book row will inherit the value in this row.") // Identifier
      generator.writeString("") // Enable for sale?
      generator.writeString("") // Title
      generator.writeString("") // Subtitle
      generator.writeString("Digital") // Book format
      generator.writeString("") // Related identifier
      generator.writeString("") // Contributor
      generator.writeString("") // Biographical note
      generator.writeString("") // Language
      generator.writeString("") // Subject code
      generator.writeString("") // Age-group
      generator.writeString("")
      generator.writeString("") // Publication date
      generator.writeString("") // Page count
      generator.writeString("") // Series name
      generator.writeString("") // Volume in series
      generator.writeString("100%") // Preview type
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
      generator.writeString("No") // Include scanned pages?
      generator.writeString("No") // For mature audience?
      generator.writeString("0%") // Copy paste percentage
      generator.writeString("0.00") // USD [Recommended Retail Price, Excluding Tax] Price
      generator.writeString("WORLD") // Countries for USD [Recommended Retail Price, Excluding Tax] Price

      generator.writeEndArray()
    }

    def writeGooglePlay(generator: CsvGenerator, bookHit: BookHitV2): Unit = {
      val languageTag = LanguageTag(bookHit.language.code)
      readServiceV2.withIdAndLanguageForExport(bookHit.id, languageTag).foreach(book => {
        generator.writeStartArray()
        generator.writeRawValue(s"PKEY:${book.uuid}") // Identifier
        generator.writeString("Yes") // Enable for sale?
        generator.writeString(book.title) // Title
        generator.writeString("") // Subtitle
        generator.writeString("Digital") // Book format
        generator.writeString("") // Related identifier
        generator.writeString(asGooglePlayContributors(book.contributors).mkString(";")) // Contributor
        generator.writeString("") // Biographical note
        generator.writeString(languageTag.language.part2b.getOrElse(languageTag.language.id)) // Language
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
        generator.writeString(book.publisher.name) // Publisher name
        generator.writeString("https://www.digitallibrary.io/") // Publisher website
        generator.writeString("Yes") // Show pictures in Preview
        generator.writeString("No") // PDF Download allowed
        generator.writeString("") // On sale date
        generator.writeString("No") // DRM Enabled?
        generator.writeString("Yes") // Show photos in eBook?
        generator.writeString("Yes") // Include scanned pages?
        generator.writeString("No") // For mature audience?
        generator.writeString("0%") // Copy paste percentage
        generator.writeString("0.00") // USD [Recommended Retail Price, Excluding Tax] Price
        generator.writeString("WORLD") // Countries for USD [Recommended Retail Price, Excluding Tax] Price

        generator.writeEndArray()
      })
    }

    def asGooglePlayContributors(contributors: Seq[api.Contributor]): Seq[String] = {
      contributors.map(c => s"${c.name} [${c.`type`}]")
    }
  }
}
