/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import org.json4s.Formats
import scalikejdbc._

case class Feed (id: Option[Long],
                 revision: Option[Int],
                 url: String,
                 uuid: String)

object Feed extends SQLSyntaxSupport[Feed] {
  implicit val formats: Formats = org.json4s.DefaultFormats
  override val tableName = "feed"
  override val schemaName = Some(BookApiProperties.MetaSchema)
  override val columns = Seq("id", "revision", "url", "uuid")


  def apply(feed: SyntaxProvider[Feed])(rs: WrappedResultSet): Feed = apply(feed.resultName)(rs)
  def apply(feed: ResultName[Feed])(rs: WrappedResultSet): Feed = Feed(
    rs.longOpt(feed.id),
    rs.intOpt(feed.revision),
    rs.string(feed.url),
    rs.string(feed.uuid)
  )
}
