/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import com.amazonaws.services.s3.model.{GetObjectRequest, S3Object}
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain.BookFormat
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
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

  test("that file is generated on the fly") {
    val pdfRendererBuilder = mock[PdfRendererBuilder]
    val uuid = "dummy-epub-uuid"
    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(bookRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultBook))
    when(readService.chaptersForIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Seq(TestData.Api.ChapterSummary1))
    when(readService.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    val pdf = pdfService.getPdf(LanguageTag("eng"), uuid)
    pdf.get.fileName should be ("Default translation title.pdf")
  }

  test("that file is fetched from s3") {
    val s3Object = mock[S3Object]
    val uuid = "dummy-pdf-uuid"
    when(translationRepository.withUuId(uuid)).thenReturn(Some(TestData.Domain.DefaultTranslation.copy(bookFormat = BookFormat.PDF)))
    when(amazonClient.getObject(any[GetObjectRequest])).thenReturn(s3Object)
    val pdf = pdfService.getPdf(LanguageTag("eng"), uuid)
    pdf.get.fileName should be ("Default translation title.pdf")
  }
}
