/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.search

import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, verify}

import scala.util.Success

class IndexBuilderServiceTest  extends UnitSuite with TestEnvironment{
  val service = new IndexBuilderService

  test("that surplus indexes are deleted") {
    when(indexService.findAllIndexes()).thenReturn(Success(Seq("books-nb_timestamp_uuid", "books-nn_timestamp_uuid")))
    when(indexService.deleteSearchIndex(any[Option[String]])).thenReturn(Success())

    service.deleteOldIndexes(Seq(LanguageTag(TestData.LanguageCodeNorwegian)))
    verify(indexService).deleteSearchIndex(Some("books-nn_timestamp_uuid"))
  }
}
