/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
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
  this: ChapterRepository =>
  val mergeService: MergeService

  class MergeService extends LazyLogging {

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
      val originalDoc = Jsoup.parseBodyFragment(originalChapter.content)

      val translatedDoc = Jsoup.parseBodyFragment(
        Jsoup.clean(translatedChapter.content, Whitelist.basicWithImages().removeTags("a"))
      )

      val embedElements = originalDoc.select("embed[data-resource]").asScala
      val imgElements = translatedDoc.getElementsByTag("img").asScala

      embedElements.zipAll(imgElements, "", "").foreach {
        case (embed: Element, img: Element) => img.replaceWith(embed)
        case (_, img: Element) => img.remove()
        case (_, _) => Unit
      }

      originalChapter.copy(content = translatedDoc.body().html())

    }
  }
}
