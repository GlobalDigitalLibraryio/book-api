/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import coza.opencollab.epub.creator.model.contributor.Contributor
import coza.opencollab.epub.creator.model.{Content, EpubBook, TocLink}
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.integration.{DownloadedMedia, ImageApiClient, MediaApiClient}
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api.NotFoundException
import io.digitallibrary.bookapi.model.domain.{BookFormat, BookType, ContributorType, EPubChapter, EPubCss}
import io.digitallibrary.bookapi.repository.{ChapterRepository, TranslationRepository}

import scala.util.{Failure, Success, Try}

trait EPubService {
  this: TranslationRepository with ChapterRepository with ContentConverter with MediaApiClient with ImageApiClient =>
  val ePubService: EPubService

  class EPubService() extends LazyLogging {
    def createEPub(language: LanguageTag, uuid: String): Option[Try[EpubBook]] = {
      unFlaggedTranslationsRepository.withUuId(uuid).map(translation =>
        translation.bookFormat match {
          case BookFormat.HTML => buildEPubFor(translation, chapterRepository.chaptersForBookIdAndLanguage(translation.bookId, language))
          case BookFormat.PDF => Failure(new NotFoundException(s"Book ${translation.title} is not an epub book."))
        }
      )
    }

    private def bookContainsHref(book: EpubBook, href: String): Boolean = {
      import scala.collection.JavaConverters._
      book.getContents.asScala.exists(_.getHref == href)
    }

    private def buildEPubFor(translation: domain.Translation, chapters: Seq[domain.Chapter]): Try[EpubBook] = {
      Try {
        val book = new EpubBook(translation.language.toString, translation.uuid, translation.title)
        book.setDescription(translation.about)

        translation.contributors.foreach(contributor => {
          val ctbType = contributor.`type` match {
            case ContributorType.Author => coza.opencollab.epub.creator.model.contributor.ContributorType.Author
            case ContributorType.Illustrator => coza.opencollab.epub.creator.model.contributor.ContributorType.Illustrator
            case ContributorType.Translator => coza.opencollab.epub.creator.model.contributor.ContributorType.Translator
            case _ => null
          }

          book.addContributor(new Contributor(contributor.person.name, ctbType))
        })


        // Add CSS to ePub
        val ePubCss = EPubCss()
        val css = new Content(ePubCss.mimeType, ePubCss.href, "css", null, ePubCss.asBytes)
        css.setToc(false)
        css.setSpine(false)
        book.addContent(css)

        // Add CoverPhoto if defined, throw exception when trouble
        translation.coverphoto.foreach(coverPhotoId => {
          downloadMedia(coverPhotoId, translation.language.toString, None, Some("IMAGE") ) match {
            case Failure(ex) => throw ex
            case Success(downloadedImage) =>
              val coverImage = new Content(
                downloadedImage.contentType,
                downloadedImage.filename,
                "cover",
                "cover-image",
                downloadedImage.bytes)

              coverImage.setSpine(false)
              book.addContent(coverImage)
          }
        })
        // Add images first (do not add more than one version of each image if image is used in multiple pages)
        val mediaIdsToAdd = chapters.flatMap(_.mediaInChapter()).distinct
        val mediaType = if (translation.bookType == BookType.BOOK) "IMAGE" else translation.bookType.toString

        val medias = mediaIdsToAdd.map(idAndSize => downloadMedia(idAndSize._1, translation.language.toString, idAndSize._2, Some(mediaType))).map(_.get)

        if(translation.bookType == BookType.BOOK) {
          for (imageWithIndex <- medias.zipWithIndex) {
            val image = imageWithIndex._1
            val imageNo = imageWithIndex._2

            val epubImage = new Content(
              image.contentType,
              image.filename,
              s"image-${image.id}-$imageNo",
              null,
              image.bytes)
            epubImage.setToc(false)
            epubImage.setSpine(false)

            if (!bookContainsHref(book, epubImage.getHref)) {
              book.addContent(epubImage)
            }
          }
        } else {
          for (videoWithIndex <- medias.zipWithIndex) {
            val video = videoWithIndex._1
            val videoNo = videoWithIndex._2

            val epubVideo = new Content(
              video.contentType,
              video.filename,
              s"video-${video.id}-$videoNo",
              null,
              video.bytes)
            epubVideo.setToc(false)
            epubVideo.setSpine(false)

            if (!bookContainsHref(book, epubVideo.getHref)) {
              book.addContent(epubVideo)
            }
          }
        }

        // Add chapter, and return Table Of Content links for each chapter
        val tocLinks = chapters.map(chapter => {
          val ePubChapter = EPubChapter(
            chapter.seqNo,
            contentConverter.toEPubContent(chapter.content, medias),
            ePubCss)

          book.addContent(new Content(ePubChapter.mimeType, ePubChapter.href, ePubChapter.id, null, ePubChapter.asBytes))
          new TocLink(ePubChapter.href, ePubChapter.title, ePubChapter.title)
        })

        book.setTocLinks(scala.collection.JavaConverters.seqAsJavaList(tocLinks))
        book.setAutoToc(false)
        book
      }
    }

  private def downloadMedia(id: Long, language: String, width: Option[Int] = None, mediaType: Option[String] = Some("IMAGE")) = {
      mediaType match {
        case Some("IMAGE") => mediaApiClient.downloadImage(id, language, width) match {
          case Success(x) => Success(x)
          case Failure(_) => imageApiClient.downloadImage(id, width)
        }
        case Some("VIDEO") => mediaApiClient.downloadVideo(id, language, width) match {
          case Success(x) => Success(x)
          case Failure(ex) => Failure(ex)
        }
        case None => Failure(new NotFoundException(s"${mediaType.get} with id $id was not found"))
      }
    }
  }

}
