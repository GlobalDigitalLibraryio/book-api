/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.OptimisticLockException
import scalikejdbc._

case class Chapter(id: Option[Long],
                   revision: Option[Int],
                   translationId: Long,
                   seqNo: Int,
                   title: Option[String],
                   content: String)

object Chapter extends SQLSyntaxSupport[Chapter] {


  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "chapter"
  override val schemaName = Some(BookApiProperties.MetaSchema)
  private val (ch, t) = (Chapter.syntax, Translation.syntax)

  def apply(ch: SyntaxProvider[Chapter])(rs: WrappedResultSet): Chapter = apply(ch.resultName)(rs)

  def apply(ch: ResultName[Chapter])(rs: WrappedResultSet): Chapter = Chapter(
    rs.longOpt(ch.id),
    rs.intOpt(ch.revision),
    rs.long(ch.translationId),
    rs.int(ch.seqNo),
    rs.stringOpt(ch.title),
    rs.string(ch.content)
  )

  def opt(ch: SyntaxProvider[Chapter])(rs: WrappedResultSet): Option[Chapter] =
    rs.longOpt(ch.resultName.id).map(_ => Chapter(ch)(rs))

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
      .where.eq(t.bookId, bookId).and.eq(t.language, language).toSQL
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