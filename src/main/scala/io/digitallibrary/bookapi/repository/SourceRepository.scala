package io.digitallibrary.bookapi.repository

import io.digitallibrary.bookapi.model.domain.{UnflaggedTranslations, Book, Source}
import io.digitallibrary.language.model.LanguageTag
import scalikejdbc._

trait SourceRepository {
  val sourceRepository: SourceRepository

  class SourceRepository {
    private val (book, t) = (Book.syntax, UnflaggedTranslations.syntax)

    def getSources(language: LanguageTag)(implicit  session: DBSession = ReadOnlyAutoSession): Seq[Source] = {
      select(book.source, sqls.count)
        .from(Book as book)
        .innerJoin(UnflaggedTranslations as t).on(book.id, t.bookId)
        .where.eq(t.language, language.toString())
        .groupBy(book.source)
        .toSQL
        .map(rs => Source(rs.string(1), rs.long(2))).list().apply()
    }
  }
}
