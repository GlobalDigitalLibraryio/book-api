/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api.OptimisticLockException
import io.digitallibrary.bookapi.model.domain._
import scalikejdbc._


trait ChapterRepository {
  val chapterRepository: ChapterRepository

  class ChapterRepository(translationView: TranslationView = UnflaggedTranslations) {
    def deleteChapter(chapter: Chapter)(implicit session: DBSession = AutoSession): Unit = {
      val ch = Chapter.column
      deleteFrom(Chapter).where.eq(ch.id, chapter.id).toSQL.update().apply()
    }

    private val (ch, t) = (Chapter.syntax, translationView.syntax)

    def add(newChapter: Chapter)(implicit session: DBSession = AutoSession): Chapter = {
      val ch = Chapter.column
      val startRevision = 1

      val id = insert.into(Chapter).namedValues(
        ch.translationId -> newChapter.translationId,
        ch.seqNo -> newChapter.seqNo,
        ch.title -> newChapter.title,
        ch.content -> newChapter.content,
        ch.chapterType -> newChapter.chapterType.toString
      ).toSQL.updateAndReturnGeneratedKey().apply()

      newChapter.copy(id = Some(id), revision = Some(startRevision))
    }

    def updateChapter(chapter: Chapter)(implicit session: DBSession = AutoSession): Chapter = {
      val ch = Chapter.column
      val nextRevision = chapter.revision.getOrElse(0) + 1

      val count = update(Chapter).set(
        ch.revision -> nextRevision,
        ch.title -> chapter.title,
        ch.content -> chapter.content,
        ch.chapterType -> chapter.chapterType.toString
      ).where
        .eq(ch.id, chapter.id).and
        .eq(ch.revision, chapter.revision).toSQL.update().apply()

      if(count != 1) {
        throw new OptimisticLockException()
      } else {
        chapter.copy(revision = Some(nextRevision))
      }
    }

    def withId(chapterId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Chapter] = {
      select
        .from(Chapter as ch)
        .where.eq(ch.id, chapterId).toSQL
        .map(Chapter(ch)).single().apply()
    }

    def chaptersForBookIdAndLanguage(bookId: Long, language: LanguageTag)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Chapter] = {
      select
        .from(Chapter as ch)
        .leftJoin(translationView as t).on(ch.translationId, t.id)
        .where.eq(t.bookId, bookId).and.eq(t.language, language.toString)
        .orderBy(ch.seqNo).asc
        .toSQL
        .map(Chapter(ch)).list().apply()

    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: LanguageTag, chapterId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Chapter] = {
      select
        .from(Chapter as ch)
        .rightJoin(translationView as t).on(ch.translationId, t.id)
        .where
        .eq(t.bookId, bookId).and
        .eq(t.language, language.toString).and
        .eq(ch.id, chapterId).toSQL
        .map(Chapter(ch)).single().apply()
    }

    def forTranslationWithSeqNo(translationId: Long, seqno: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Chapter] = {
      select
        .from(Chapter as ch)
        .rightJoin(translationView as t).on(ch.translationId, t.id)
        .where
        .eq(t.id, translationId).and
        .eq(ch.seqNo, seqno).toSQL
        .map(Chapter(ch)).single().apply()
    }

    def deleteChaptersExceptGivenSeqNumbers(translationId: Long, seqNumbersToKeep: Seq[Int])(implicit session: DBSession = AutoSession): Unit = {
      val ch = Chapter.column
      deleteFrom(Chapter)
        .where
        .eq(ch.translationId, translationId).and
        .notIn(ch.seqNo, seqNumbersToKeep)
        .toSQL.update().apply()
    }
  }
}
