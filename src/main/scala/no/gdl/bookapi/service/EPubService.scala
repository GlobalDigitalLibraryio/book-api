/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import java.text.MessageFormat

import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import coza.opencollab.epub.creator.model.{Content, EpubBook, TocLink}
import no.gdl.bookapi.integration.ImageApiClient
import no.gdl.bookapi.model._
import no.gdl.bookapi.repository.{ChapterRepository, TranslationRepository}

import scala.util.{Failure, Success, Try}

trait EPubService {
  this: TranslationRepository with ChapterRepository with ContentConverter with ImageApiClient =>
  val ePubService: EPubService

  class EPubService(cssTemplate: String, chapterTemplate: String) extends LazyLogging {
    private val EpubCssHref = "epub.css"
    private val EpubCssMimeType = "text/css"
    private val EpubXhtmlMimeType = "application/xhtml+xml"

    private def ChapterTitleTemplate(seqNo: Int) = s"Chapter $seqNo"

    private def ChapterHref(seqNo: Int) = s"chapter-$seqNo.xhtml"

    def createEPub(language: String, uuid: String): Option[Try[EpubBook]] = {
      translationRepository.withUuId(uuid).map(translation =>
        buildEPubFor(translation, chapterRepository.chaptersForBookIdAndLanguage(translation.bookId, language)))
    }

    private def buildEPubFor(translation: domain.Translation, chapters: Seq[domain.Chapter]): Try[EpubBook] = {
      Try {
        val authors = translation.contributors.filter(_.`type` == "Author").map(_.person.name).mkString(",")
        val book = new EpubBook(translation.language, translation.uuid, translation.title, authors)

        // Add CSS to ePub
        book.addContent(cssTemplate.getBytes(), EpubCssMimeType, EpubCssHref, false, false)

        // Add CoverPhoto if defined, throw exception when trouble
        translation.coverphoto.foreach(coverPhotoId => {
          imageApiClient.downloadImage(coverPhotoId) match {
            case Failure(ex) => throw ex
            case Success(downloadedImage) =>
              book.addCoverImage(
                downloadedImage.bytes,
                downloadedImage.metaInformation.contentType,
                downloadedImage.metaInformation.domainUrl.pathParts.last.part)
          }})

        // Add chapter, and return Table Of Content links for each chapter
        val tocLinks = chapters.map(chapter => {

          // Download all images, throw exception if download fails
          val images = chapter.imagesInChapter().map(imageApiClient.downloadImage).map(_.get)
          images.foreach(image => {
            book.addContent(
              image.bytes,
              image.metaInformation.contentType,
              image.metaInformation.domainUrl.pathParts.last.part,
              false, false)})

          val content = contentConverter.toEPubContent(chapter.content, images)
          val title = chapter.title.getOrElse(ChapterTitleTemplate(chapter.seqNo))
          val href = ChapterHref(chapter.seqNo)
          val htmlContent = MessageFormat.format(chapterTemplate, title, content)
          book.addContent(new Content(EpubXhtmlMimeType, href, htmlContent.getBytes))

          new TocLink(href, title, title)
        })

        book.setTocLinks(scala.collection.JavaConverters.seqAsJavaList(tocLinks))
        book.setAutoToc(false)
        book
      }
    }
  }
}
