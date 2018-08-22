/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import java.io.ByteArrayOutputStream
import java.time.LocalDate

import io.digitallibrary.bookapi.model.api.{BookHit, Language, SearchResult}
import io.digitallibrary.bookapi.model.domain.{Paging, Sort}
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito

class ExportServiceTest extends UnitSuite with TestEnvironment {
  val service = new ExportService

  test("that export of books works as intended") {
    val outputStream = new ByteArrayOutputStream()
    Mockito.when(searchService.searchWithQuery(any[LanguageTag], any[Option[String]], any[Option[String]], any[Paging], any[Sort.Value]))
      .thenReturn(SearchResult(1L, 1, 1, Some(Language(TestData.LanguageCodeEnglish, "")),
        Seq(BookHit(1, "This is a title", "Short description", Language(TestData.LanguageCodeEnglish, ""), None, Seq(), None, LocalDate.now(), "source", None, None)))
      )
    service.exportBooks(LanguageTag(TestData.LanguageCodeEnglish), Some("all"), outputStream)
    assert(outputStream.toString.contains("id,environment,language,title,description,source,url,approved,comment"))
    assert(outputStream.toString.contains("This is a title"))
    assert(outputStream.toString.contains("Short description"))
  }

  test("that export of no books gives csv with headers") {
    val outputStream = new ByteArrayOutputStream()
    Mockito.when(searchService.searchWithQuery(any[LanguageTag], any[Option[String]], any[Option[String]], any[Paging], any[Sort.Value]))
      .thenReturn(SearchResult(0L, 1, 0, Some(Language(TestData.LanguageCodeEnglish, "")), Seq())
      )
    service.exportBooks(LanguageTag(TestData.LanguageCodeEnglish), Some("all"), outputStream)
    assert(outputStream.toString.contains("id,environment,language,title,description,source,url,approved,comment"))
  }

}
