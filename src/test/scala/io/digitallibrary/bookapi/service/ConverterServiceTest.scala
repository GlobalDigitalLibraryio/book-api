/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.domain.{BookFormat, Translation}
import io.digitallibrary.bookapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito.when

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new ConverterService

  test("that toApiDownloads creates URLs as expected") {
    val translation = mock[Translation]
    when(translation.language).thenReturn(LanguageTag("eng"))
    when(translation.uuid).thenReturn("12345")
    when(translation.bookFormat).thenReturn(BookFormat.HTML)

    val downloads = service.toApiDownloads(translation)
    downloads.epub should equal(Some("http://local.digitallibrary.io/book-api/download/epub/eng/12345.epub"))
    downloads.pdf should equal(Some("http://local.digitallibrary.io/book-api/download/pdf/eng/12345.pdf"))

    when(translation.bookFormat).thenReturn(BookFormat.PDF)
    val download2 = service.toApiDownloads(translation)
    download2.pdf should equal(Some("http://local.digitallibrary.io/book-api/download/pdf/eng/12345.pdf"))
    download2.epub should equal(None)
  }

}
