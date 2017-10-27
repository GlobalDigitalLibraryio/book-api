/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import no.gdl.bookapi.model.domain.Translation
import no.gdl.bookapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito.when

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new ConverterService

  test("that toApiDownloads creates URLs as expected") {
    val translation = mock[Translation]
    when(translation.language).thenReturn("eng")
    when(translation.uuid).thenReturn("12345")

    val downloads = service.toApiDownloads(translation)
    downloads.epub should equal("http://local.digitallibrary.io/book-api/download/epub/eng/12345.epub")
    downloads.pdf should equal("http://local.digitallibrary.io/book-api/download/pdf/eng/12345.pdf")
  }

}
