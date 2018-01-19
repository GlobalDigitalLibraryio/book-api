/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.integration.crowdin.TranslatedChapter
import no.gdl.bookapi.model.api.Book
import no.gdl.bookapi.model.api.internal.NewChapter
import no.gdl.bookapi.model.domain.Chapter
import no.gdl.bookapi.repository.ChapterRepository

trait MergeService {
  this: ChapterRepository =>
  val mergeService: MergeService

  class MergeService extends LazyLogging {
    def mergeChapters(originalBook: Book, translatedChapters: Seq[TranslatedChapter]): Seq[NewChapter] = {
      val originalChapters = originalBook.chapters.flatMap(chapter => chapterRepository.withId(chapter.id))

      translatedChapters.flatMap(
        chapter => originalChapters.find(_.id.contains(chapter.id)).map(
          originalChapter => mergeChapter(originalChapter, chapter)))
    }

    def mergeChapter(originalChapter: Chapter, chapter: TranslatedChapter): NewChapter = {
      // TODO: Implement actual merging
      NewChapter(originalChapter.seqNo, None, chapter.content)
    }

  }
}
