/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.time.LocalDate
import java.util.zip.{ZipEntry, ZipInputStream}

import io.digitallibrary.bookapi.model.api.{BookHit, Downloads, Language, SearchResult}
import io.digitallibrary.bookapi.model.domain.{CsvFormat, Paging, Sort}
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import scalaj.http.{Http, HttpOptions, HttpRequest}

import scala.util.{Failure, Success}

class ExportServiceTest extends UnitSuite with TestEnvironment {
  val service = new ExportService

  test("that export of books works as intended") {
    val outputStream = new ByteArrayOutputStream()
    Mockito.when(searchService.searchWithQuery(any[LanguageTag], any[Option[String]], any[Option[String]], any[Paging], any[Sort.Value]))
      .thenReturn(SearchResult(1L, 1, 1, Some(Language(TestData.LanguageCodeEnglish, "")),
        Seq(BookHit(1, "This is a title", "Short description", Language(TestData.LanguageCodeEnglish, ""), None, Seq(), None, LocalDate.now(), "source", None, None)))
      )
    service.exportBooks(CsvFormat.QualityAssurance, LanguageTag(TestData.LanguageCodeEnglish), Some("all"), outputStream)
    assert(outputStream.toString.contains("id,environment,language,title,description,source,url,approved,comment"))
    assert(outputStream.toString.contains("This is a title"))
    assert(outputStream.toString.contains("Short description"))
  }

  test("that export of no books gives csv with headers") {
    val outputStream = new ByteArrayOutputStream()
    Mockito.when(searchService.searchWithQuery(any[LanguageTag], any[Option[String]], any[Option[String]], any[Paging], any[Sort.Value]))
      .thenReturn(SearchResult(0L, 1, 0, Some(Language(TestData.LanguageCodeEnglish, "")), Seq()))

    service.exportBooks(CsvFormat.QualityAssurance, LanguageTag(TestData.LanguageCodeEnglish), Some("all"), outputStream)
    assert(outputStream.toString.contains("id,environment,language,title,description,source,url,approved,comment"))
  }

  test("that export of epubs creates a zip archive with all files") {
    val outputStream = new ByteArrayOutputStream()
    Mockito.when(searchService.searchWithQuery(any[LanguageTag], any[Option[String]], any[Option[String]], any[Paging], any[Sort.Value]))
      .thenReturn(SearchResult(2L, 1, 2, Some(Language(TestData.LanguageCodeEnglish, "")),
        Seq(
          BookHit(1, "This is the first title", "Short description", Language(TestData.LanguageCodeEnglish, ""), None, Seq(), None, LocalDate.now(), "source", None, None),
          BookHit(2, "This is the second title", "Short description", Language(TestData.LanguageCodeEnglish, ""), None, Seq(), None, LocalDate.now(), "source", None, None)))
      )

    Mockito.when(readService.withIdAndLanguageForExport(eqTo(1L), any[LanguageTag])).thenReturn(Some(TestData.Internal.DefaultInternalBook.copy(uuid = "book-1-uuid", downloads = Downloads(Some("http://url-to-epub"), None))))
    Mockito.when(readService.withIdAndLanguageForExport(eqTo(2L), any[LanguageTag])).thenReturn(Some(TestData.Internal.DefaultInternalBook.copy(uuid = "book-2-uuid", downloads = Downloads(Some("http://url-to-epub"), None))))
    Mockito.when(gdlClient.fetchBytes(any[HttpRequest])).thenReturn(Success("Content".getBytes))

    service.getAllEPubsAsZipFile(LanguageTag("en"), Some("storyweaver"), outputStream)

    val input = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray))
    var entry: Option[ZipEntry] = None
    var entries: Seq[ZipEntry] = Seq()
    while({entry = Option(input.getNextEntry); entry}.isDefined) { entries = entries ++ entry }

    entries.size should be (2)
    entries.head.getName should equal ("book-1-uuid.epub")
    entries.last.getName should equal ("book-2-uuid.epub")
  }

  test("that export of epubs is still successfull even if one epub fails to download") {
    val outputStream = new ByteArrayOutputStream()
    Mockito.when(searchService.searchWithQuery(any[LanguageTag], any[Option[String]], any[Option[String]], any[Paging], any[Sort.Value]))
      .thenReturn(SearchResult(2L, 1, 2, Some(Language(TestData.LanguageCodeEnglish, "")),
        Seq(
          BookHit(1, "This is the first title", "Short description", Language(TestData.LanguageCodeEnglish, ""), None, Seq(), None, LocalDate.now(), "source", None, None),
          BookHit(2, "This is the second title", "Short description", Language(TestData.LanguageCodeEnglish, ""), None, Seq(), None, LocalDate.now(), "source", None, None)))
      )

    val url1 = "http://url-to-epub-1"
    val url2 = "http://url-to-epub-2"
    Mockito.when(readService.withIdAndLanguageForExport(eqTo(1L), any[LanguageTag])).thenReturn(Some(TestData.Internal.DefaultInternalBook.copy(uuid = "book-1-uuid", downloads = Downloads(Some(url1), None))))
    Mockito.when(readService.withIdAndLanguageForExport(eqTo(2L), any[LanguageTag])).thenReturn(Some(TestData.Internal.DefaultInternalBook.copy(uuid = "book-2-uuid", downloads = Downloads(Some(url2), None))))

    Mockito.when(gdlClient.fetchBytes(any[HttpRequest]))
      .thenReturn(Failure(new RuntimeException("Some-error")))
      .thenReturn(Success("Content".getBytes))


    service.getAllEPubsAsZipFile(LanguageTag("en"), Some("storyweaver"), outputStream)

    val input = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray))
    var entry: Option[ZipEntry] = None
    var entries: Seq[ZipEntry] = Seq()
    while({entry = Option(input.getNextEntry); entry}.isDefined) { entries = entries ++ entry }

    entries.size should be (1)
    entries.last.getName should equal ("book-2-uuid.epub")
  }

}
