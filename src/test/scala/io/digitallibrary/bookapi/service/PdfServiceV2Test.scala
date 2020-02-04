package io.digitallibrary.bookapi.service

import com.amazonaws.services.s3.model.{GetObjectRequest, S3Object}
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.bookapi.model.domain.BookFormat
import io.digitallibrary.language.model.LanguageTag
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import scalikejdbc.DBSession

class PdfServiceV2Test extends UnitSuite with TestEnvironment {

  override val pdfServiceV2 = new PdfServiceV2

  test("v2: that None is returned when non-existing uuid") {
    when(unFlaggedTranslationsRepository.withUuId(any[String])(any[DBSession])).thenReturn(None)
    pdfServiceV2.createPdf(LanguageTag("eng"), "not-existing-uuid") should be (None)
  }

  test("v2: that a renderer is returned when book is found") {
    when(unFlaggedTranslationsRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(bookRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultBook))
    when(readServiceV2.chaptersForIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Seq(TestData.Api.ChapterSummary1))
    when(readServiceV2.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long], any[Boolean])).thenReturn(Some(TestData.ApiV2.Chapter1))


    val renderer = pdfServiceV2.createPdf(LanguageTag("amh"), "123-144-155")
    renderer.isDefined should be (true)
  }

  test("v2: that file is generated on the fly") {
    val pdfRendererBuilder = mock[PdfRendererBuilder]
    val uuid = "dummy-epub-uuid"
    when(unFlaggedTranslationsRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(bookRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultBook))
    when(readServiceV2.chaptersForIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Seq(TestData.Api.ChapterSummary1))
    when(readServiceV2.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long], any[Boolean])).thenReturn(Some(TestData.ApiV2.Chapter1))

    val pdf = pdfServiceV2.getPdf(LanguageTag("eng"), uuid)
    pdf.get.fileName should be ("Default translation title.pdf")
  }

  test("v2: that file is fetched from s3") {
    val s3Object = mock[S3Object]
    val uuid = "dummy-pdf-uuid"
    when(unFlaggedTranslationsRepository.withUuId(uuid)).thenReturn(Some(TestData.Domain.DefaultTranslation.copy(bookFormat = BookFormat.PDF)))
    when(amazonS3Client.getObject(any[GetObjectRequest])).thenReturn(s3Object)
    val pdf = pdfServiceV2.getPdf(LanguageTag("eng"), uuid)
    pdf.get.fileName should be ("Default translation title.pdf")
  }
}
