/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.license.model.License
import scalikejdbc._


case class Book(id: Option[Long],
                revision: Option[Int],
                publisherId: Long,
                publisher: Publisher,
                license: License,
                source: String)

object Book extends SQLSyntaxSupport[Book] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "book"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(b: SyntaxProvider[Book], pub: SyntaxProvider[Publisher])(rs: WrappedResultSet): Book =
    apply(b.resultName, pub.resultName)(rs)


  def apply(b: ResultName[Book], pub: ResultName[Publisher])(rs: WrappedResultSet): Book = Book(
      rs.longOpt(b.id),
      rs.intOpt(b.revision),
      rs.long(b.publisherId),
      Publisher.apply(pub)(rs),
      License(rs.string(b.license)),
      rs.string(b.source))

}
