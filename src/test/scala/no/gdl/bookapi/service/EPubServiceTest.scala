/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.{DownloadedImage, ImageMetaInformation}
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers.{any, eq => eqTo}
import scalikejdbc.DBSession

import scala.collection.JavaConverters
import scala.util.{Failure, Success}

class EPubServiceTest extends UnitSuite with TestEnvironment {

  override val ePubService = new EPubService

  test("that createEPub returns None when uuid not found") {
    when(translationRepository.withUuId("123")).thenReturn(None)
    ePubService.createEPub(LanguageTag("nob"), "123") should equal (None)
  }


  test("that createEPub creates a book with expected metadata") {
    val uuid = TestData.Domain.DefaultTranslation.uuid
    val language = TestData.Domain.DefaultTranslation.language

    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(chapterRepository.chaptersForBookIdAndLanguage(any[Long], any[LanguageTag])(any[DBSession])).thenReturn(Seq())

    ePubService.createEPub(language, uuid) match {
      case None => fail("epub should be defined")
      case Some(Failure(ex)) => fail("epub should be a success")
      case Some(Success(book)) => {
        book.getTitle should equal (TestData.Domain.DefaultTranslation.title)
        book.getId should equal (uuid)
        book.getLanguage should equal (language.toString)
        book.getAuthor should equal (TestData.Domain.DefaultTranslation.contributors.map(_.person.name).mkString(","))
      }
    }
  }

  test("that createEPub creates a book with expected CoverPhoto") {
    val translation = TestData.Domain.DefaultTranslation.copy(coverphoto = Some(1))
    val image = DownloadedImage(ImageMetaInformation("1", "meta-url", "image-url", 1, "image/png"), "bytes".getBytes)

    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(translation))
    when(chapterRepository.chaptersForBookIdAndLanguage(any[Long], any[LanguageTag])(any[DBSession])).thenReturn(Seq())
    when(imageApiClient.downloadImage(translation.coverphoto.get)).thenReturn(Success(image))

    ePubService.createEPub(translation.language, translation.uuid) match {
      case None => fail("epub should be defined")
      case Some(Failure(ex)) => fail("epub should be a success")
      case Some(Success(book)) => {
        val contents = JavaConverters.asScalaBuffer(book.getContents).toSeq
        val coverImage = contents.find(_.getProperties == "cover-image").get
        coverImage.getMediaType should equal (image.metaInformation.contentType)
      }
    }
  }

  test("that createEpub returns failure when download of coverphoto fails") {
    val translation = TestData.Domain.DefaultTranslation.copy(coverphoto = Some(1))

    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(translation))
    when(chapterRepository.chaptersForBookIdAndLanguage(any[Long], any[LanguageTag])(any[DBSession])).thenReturn(Seq())
    when(imageApiClient.downloadImage(translation.coverphoto.get)).thenReturn(Failure(new RuntimeException("error")))

    ePubService.createEPub(translation.language, translation.uuid) match {
      case None => fail("epub should be defined")
      case Some(Success(book)) => fail("should be a failure")
      case Some(Failure(ex)) => {
        ex.getMessage should equal ("error")
      }
    }
  }

  test("that createEpub creates a book with expected chapter") {
    val translation = TestData.Domain.DefaultTranslation
    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(translation))
    when(chapterRepository.chaptersForBookIdAndLanguage(any[Long], any[LanguageTag])(any[DBSession])).thenReturn(translation.chapters)

    when(contentConverter.toEPubContent(any[String], any[Seq[DownloadedImage]])).thenReturn("Content-of-chapter")

    ePubService.createEPub(translation.language, translation.uuid) match {
      case None => fail("epub should be defined")
      case Some(Failure(ex)) => fail("epub should be a success")
      case Some(Success(book)) => {
        val contents = JavaConverters.asScalaBuffer(book.getContents).toSeq
        contents.size should be (2)
        contents.exists(_.getHref == s"chapter-${translation.chapters.head.seqNo}.xhtml") should be (true)

      }
    }
  }

  test("that createEpub creates a book with expected chapter with image") {
    val translation = TestData.Domain.DefaultTranslation
    val chapter = TestData.Domain.DefaultChapter.copy(content = """<p><embed data-resource="image" data-resource_id="1"/></p>""")
    val image = DownloadedImage(ImageMetaInformation("1", "meta-url", "image-url", 1, "image/png"), "bytes".getBytes)

    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(translation))
    when(chapterRepository.chaptersForBookIdAndLanguage(any[Long], any[LanguageTag])(any[DBSession])).thenReturn(Seq(chapter))

    when(contentConverter.toEPubContent(any[String], any[Seq[DownloadedImage]])).thenReturn("Content-of-chapter")
    when(imageApiClient.downloadImage(1)).thenReturn(Success(image))

    ePubService.createEPub(translation.language, translation.uuid) match {
      case None => fail("epub should be defined")
      case Some(Failure(ex)) => fail("epub should be a success")
      case Some(Success(book)) => {
        val contents = JavaConverters.asScalaBuffer(book.getContents).toSeq
        contents.size should be (3)
        contents.exists(_.getHref == s"chapter-${translation.chapters.head.seqNo}.xhtml") should be (true)
        contents.exists(_.getHref == "image-url") should be (true)
      }
    }
  }

  test("that createEpub returns failure when download of chapter-image fails") {
    val translation = TestData.Domain.DefaultTranslation
    val chapter = TestData.Domain.DefaultChapter.copy(content = """<p><embed data-resource="image" data-resource_id="1"/></p>""")

    when(translationRepository.withUuId(any[String])(any[DBSession])).thenReturn(Some(translation))
    when(chapterRepository.chaptersForBookIdAndLanguage(any[Long], any[LanguageTag])(any[DBSession])).thenReturn(Seq(chapter))
    when(imageApiClient.downloadImage(1)).thenReturn(Failure(new RuntimeException("image-download-error")))

    ePubService.createEPub(translation.language, translation.uuid) match {
      case None => fail("epub should be defined")
      case Some(Success(book)) => fail("epub should be a failure")
      case Some(Failure(ex)) => ex.getMessage should equal ("image-download-error")

    }
  }
}
