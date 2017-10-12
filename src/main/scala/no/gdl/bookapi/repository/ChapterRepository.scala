/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.{Chapter, Translation}
import scalikejdbc._


trait ChapterRepository {
  val chapterRepository: ChapterRepository

  class ChapterRepository {
    private val (ch, t) = (Chapter.syntax, Translation.syntax)

    def add(newChapter: Chapter)(implicit session: DBSession = AutoSession): Chapter = {
      val ch = Chapter.column
      val startRevision = 1

      val id = insert.into(Chapter).namedValues(
        ch.translationId -> newChapter.translationId,
        ch.seqNo -> newChapter.seqNo,
        ch.title -> newChapter.title,
        ch.content -> newChapter.content
      ).toSQL.updateAndReturnGeneratedKey().apply()

      newChapter.copy(id = Some(id), revision = Some(startRevision))
    }

    def updateChapter(chapter: Chapter)(implicit session: DBSession = AutoSession): Chapter = {
      val ch = Chapter.column
      val nextRevision = chapter.revision.getOrElse(0) + 1

      val count = update(Chapter).set(
        ch.revision -> nextRevision,
        ch.title -> chapter.title,
        ch.content -> chapter.content
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

    def chaptersForBookIdAndLanguage(bookId: Long, language: String)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Chapter] = {
      select
        .from(Chapter as ch)
        .leftJoin(Translation as t).on(ch.translationId, t.id)
        .where.eq(t.bookId, bookId).and.eq(t.language, language)
        .orderBy(ch.seqNo).asc
        .toSQL
        .map(Chapter(ch)).list().apply()

    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: String, chapterId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Chapter] = {
      select
        .from(Chapter as ch)
        .rightJoin(Translation as t).on(ch.translationId, t.id)
        .where
        .eq(t.bookId, bookId).and
        .eq(t.language, language).and
        .eq(ch.id, chapterId).toSQL
        .map(Chapter(ch)).single().apply()
    }

    def forTranslationWithSeqNo(translationId: Long, seqno: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Chapter] = {
      select
        .from(Chapter as ch)
        .rightJoin(Translation as t).on(ch.translationId, t.id)
        .where
        .eq(t.id, translationId).and
        .eq(ch.seqNo, seqno).toSQL
        .map(Chapter(ch)).single().apply()
    }
  }
}
