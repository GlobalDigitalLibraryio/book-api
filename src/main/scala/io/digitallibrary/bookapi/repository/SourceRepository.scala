package io.digitallibrary.bookapi.repository

import io.digitallibrary.bookapi.model.domain.{AllTranslations, Book}
import io.digitallibrary.language.model.LanguageTag
import scalikejdbc._

trait SourceRepository {
  val sourceRepository: SourceRepository

  class SourceRepository {
    private val (book, t) = (Book.syntax, AllTranslations.syntax)

    def getSources(language: LanguageTag)(implicit  session: DBSession = ReadOnlyAutoSession): Seq[String] = {
      select(sqls.distinct(book.source))
        .from(Book as book)
        .innerJoin(AllTranslations as t).on(book.id, t.id)
        .where.eq(t.language, language.toString())
        .toSQL
        .map(rs => rs.string(1)).list().apply()
    }
  }
}
