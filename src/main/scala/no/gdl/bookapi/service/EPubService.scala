/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import coza.opencollab.epub.creator.model.{Content, EpubBook, TocLink}
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.ImageApiClient
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.domain.{ContributorType, EPubChapter, EPubCss}
import no.gdl.bookapi.repository.{ChapterRepository, TranslationRepository}

import scala.util.{Failure, Success, Try}

trait EPubService {
  this: TranslationRepository with ChapterRepository with ContentConverter with ImageApiClient =>
  val ePubService: EPubService

  class EPubService() extends LazyLogging {
    def createEPub(language: LanguageTag, uuid: String): Option[Try[EpubBook]] = {
      translationRepository.withUuId(uuid).map(translation =>
        buildEPubFor(translation, chapterRepository.chaptersForBookIdAndLanguage(translation.bookId, language)))
    }

    private def buildEPubFor(translation: domain.Translation, chapters: Seq[domain.Chapter]): Try[EpubBook] = {
      Try {
        val authors = translation.contributors.filter(_.`type` == ContributorType.Author).map(_.person.name).mkString(", ")
        val book = new EpubBook(translation.language.toString, translation.uuid, translation.title, authors)

        // Add CSS to ePub
        val ePubCss = EPubCss()
        book.addContent(ePubCss.asBytes, ePubCss.mimeType, ePubCss.href, false, false)

        // Add CoverPhoto if defined, throw exception when trouble
        translation.coverphoto.foreach(coverPhotoId => {
          imageApiClient.downloadImage(coverPhotoId) match {
            case Failure(ex) => throw ex
            case Success(downloadedImage) =>
              book.addCoverImage(
                downloadedImage.bytes,
                downloadedImage.metaInformation.contentType,
                downloadedImage.metaInformation.imageUrl.pathParts.last.part)
          }
        })

        // Add chapter, and return Table Of Content links for each chapter
        val tocLinks = chapters.map(chapter => {

          // Download all images, throw exception if download fails
          val images = chapter.imagesInChapter().map(imageApiClient.downloadImage).map(_.get)
          images.foreach(image => {
            book.addContent(
              image.bytes,
              image.metaInformation.contentType,
              image.metaInformation.imageUrl.pathParts.last.part,
              false, false)
          })

          val ePubChapter = EPubChapter(
            chapter.seqNo,
            contentConverter.toEPubContent(chapter.content, images),
            ePubCss)

          book.addContent(new Content(ePubChapter.mimeType, ePubChapter.href, ePubChapter.asBytes))
          new TocLink(ePubChapter.href, ePubChapter.title, ePubChapter.title)
        })

        book.setTocLinks(scala.collection.JavaConverters.seqAsJavaList(tocLinks))
        book.setAutoToc(false)
        book
      }
    }
  }
}
