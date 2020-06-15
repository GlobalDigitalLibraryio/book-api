/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.translation

import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.MessageDigest
import javax.imageio.ImageIO

import org.apache.commons.codec.binary.Hex
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.ImageApiClient
import io.digitallibrary.bookapi.integration.crowdin.TranslatedChapter
import io.digitallibrary.bookapi.model.api.Book
import io.digitallibrary.bookapi.model.api.internal.NewTranslatedChapter
import io.digitallibrary.bookapi.model.domain.{Chapter, Translation}
import io.digitallibrary.bookapi.repository.ChapterRepository
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.safety.Whitelist

import scala.collection.JavaConverters._

trait MergeService {
  this: ChapterRepository with ImageApiClient =>
  val mergeService: MergeService

  class MergeService extends LazyLogging {

    def mergeContents(originalContent: String, newContent: String, book: Option[Book]): String = {
      val originalDoc = Jsoup.parseBodyFragment(originalContent)

      val newDoc = Jsoup.parseBodyFragment(
        Jsoup.clean(newContent, Whitelist.basicWithImages().removeTags("a"))
      )

      val embedElements = originalDoc.select("embed[data-resource]").asScala
      val imgElements = newDoc.getElementsByTag("img").asScala

      book match {
        case Some(b) => {
          val embedUrls = embedElements.map { embed => imageApiClient.imageUrlFor(embed.attr("data-resource_id").toLong).get.url }
          for (img <- imgElements) {
            val imageUrl = img.attr("src")
            if (imageUrl.contains(BookApiProperties.CloudinaryCloudName)) {
              if (embedUrls.contains(imageUrl)) {
                img.replaceWith(embedElements(embedUrls.indexOf(imageUrl)))
              }
              else {
                // Upload new image to image-api
                val bufferedImage = ImageIO.read(new URL(imageUrl))
                val outputStream = new ByteArrayOutputStream()
                try {
                  ImageIO.write(bufferedImage, "jpg", outputStream)
                } catch {
                  case e: javax.imageio.IIOException => {
                    ImageIO.write(bufferedImage, "png", outputStream)
                  }
                }
                val data = outputStream.toByteArray
                outputStream.close()
                val md = MessageDigest.getInstance("MD5")
                md.update(data)
                val hash = md.digest()
                val externalId = s"${b.externalId.get}-${Hex.encodeHexString(hash)}"
                val filename = s"${b.title}-${externalId}"
                val title = s"${b.title}-${externalId}"
                val alttext = img.attr("alttext")
                val language = b.language.code
                val license = b.license.name
                val origin = b.source
                val author = b.source
                val metadata = imageApiClient.createImage(externalId, filename, title, alttext, language, license, origin, author, data)
                metadata match {
                  case Some(metadata) =>
                    val embed = "<embed data-resource=\"image\" data-resource_id=\"" + metadata.id + "\"/>";
                    img.replaceWith(Jsoup.parseBodyFragment(embed).select("embed[data-resource]").asScala.head)
                  case (_) => img.remove()
                }
              }
            }
            else{
              img.remove()
            }
          }
        }
        case (_) => {
          embedElements.zipAll(imgElements, "", "").foreach {
            case (embed: Element, img: Element) => img.replaceWith(embed)
            case (_, img: Element) => img.remove()
            case (_, _) => Unit
          }
        }
      }
      newDoc.body().html()
    }

    def mergeChapters(originalBook: Book, translatedChapters: Seq[TranslatedChapter]): Seq[NewTranslatedChapter] = {
      val originalChapters = originalBook.chapters.flatMap(chapter => chapterRepository.withId(chapter.id))

      translatedChapters.flatMap(
        chapter => originalChapters.find(_.id.contains(chapter.newChapterId)).map(
          originalChapter => mergeChapterAsNewChapter(originalChapter, chapter)))
    }

    def mergeChapters(newTranslation: Translation, newContentChapters: Seq[TranslatedChapter]): Seq[Chapter] = {
      newContentChapters.flatMap(
        newChapter => newTranslation.chapters.find(_.id == newChapter.newChapterId).map(
          originalChapter => mergeChapter(originalChapter, newChapter)))
    }

    def mergeChapterAsNewChapter(originalChapter: Chapter, translatedChapter: TranslatedChapter): NewTranslatedChapter = {
      NewTranslatedChapter(originalChapter.seqNo, None, mergeChapter(originalChapter, translatedChapter).content, originalChapter.id.get)
    }

    def mergeChapter(originalChapter: Chapter, translatedChapter: TranslatedChapter): Chapter = {
      originalChapter.copy(content = mergeContents(originalChapter.content, translatedChapter.content, None))
    }
  }
}
