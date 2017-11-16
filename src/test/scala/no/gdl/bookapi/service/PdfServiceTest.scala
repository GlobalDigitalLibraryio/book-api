/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers.{any, eq => eqTo}
import scalikejdbc.DBSession

class PdfServiceTest extends UnitSuite with TestEnvironment {

  override val pdfService = new PdfService

  test("that None is returned when non-existing uuid") {
    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(None)
    pdfService.createPdf(LanguageTag("eng"), "not-existing-uuid") should be (None)
  }

  test("that a renderer is returned when book is found") {
    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(bookRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultBook))
    when(readService.chaptersForIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Seq(TestData.Api.ChapterSummary1))
    when(readService.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Api.Chapter1))


    val renderer = pdfService.createPdf(LanguageTag("amh"), "123-144-155")
    renderer.isDefined should be (true)
  }

}
