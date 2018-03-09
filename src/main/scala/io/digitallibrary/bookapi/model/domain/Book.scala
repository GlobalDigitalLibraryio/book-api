/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

import io.digitallibrary.bookapi.BookApiProperties
import scalikejdbc._


case class Book(id: Option[Long],
                revision: Option[Int],
                publisherId: Long,
                licenseId: Long,
                publisher: Publisher,
                license: License,
                source: String)

object Book extends SQLSyntaxSupport[Book] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "book"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(b: SyntaxProvider[Book], pub: SyntaxProvider[Publisher], lic: SyntaxProvider[License])(rs: WrappedResultSet): Book =
    apply(b.resultName, pub.resultName, lic.resultName)(rs)


  def apply(b: ResultName[Book], pub: ResultName[Publisher], lic: ResultName[License])(rs: WrappedResultSet): Book = Book(
      rs.longOpt(b.id),
      rs.intOpt(b.revision),
      rs.long(b.publisherId),
      rs.long(b.licenseId),
      Publisher.apply(pub)(rs),
      License.apply(lic)(rs),
      rs.string(b.source))

}
