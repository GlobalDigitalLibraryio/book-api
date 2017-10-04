/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._

import scala.util.Try


case class Publisher(id: Option[Long],
                     revision: Option[Int],
                     name: String)

object Publisher extends SQLSyntaxSupport[Publisher] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "publisher"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(pub: SyntaxProvider[Publisher])(rs: WrappedResultSet): Publisher = apply(pub.resultName)(rs)

  def apply(pub: ResultName[Publisher])(rs: WrappedResultSet): Publisher = {
    Publisher(
      rs.longOpt(pub.id),
      rs.intOpt(pub.revision),
      rs.string(pub.name)
    )
  }
}
