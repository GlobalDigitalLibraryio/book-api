/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain
import java.time.LocalDate

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._

case class EditorsPick (id: Option[Long],
                        revision: Option[Int],
                        language: String,
                        translationIds: Seq[Long],
                        dateChanged: LocalDate)

object EditorsPick extends SQLSyntaxSupport[EditorsPick] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "editors_pick"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(ep: SyntaxProvider[EditorsPick])(rs: WrappedResultSet): EditorsPick = apply(ep.resultName)(rs)
  def apply(ep: ResultName[EditorsPick])(rs: WrappedResultSet): EditorsPick = EditorsPick(
    id = rs.longOpt(ep.id),
    revision = rs.intOpt(ep.revision),
    language = rs.string(ep.language),
    translationIds = rs.array(ep.translationIds).getArray().asInstanceOf[Array[java.lang.Long]].toSeq.map(_.toLong),
    dateChanged = rs.localDate(ep.dateChanged)
  )
}
