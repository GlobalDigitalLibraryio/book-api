/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._

case class License(id: Option[Int],
                   revision: Option[Int],
                   name: String,
                   description: Option[String],
                   url: Option[String])

object License extends SQLSyntaxSupport[License] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "license"
  override val schemaName = Some(BookApiProperties.MetaSchema)
  private val lic = syntax

  def apply(lic: SyntaxProvider[License])(rs: WrappedResultSet): License = apply(lic.resultName)(rs)
  def apply(lic: ResultName[License])(rs: WrappedResultSet): License = {
    License(
      rs.intOpt(lic.id),
      rs.intOpt(lic.revision),
      rs.string(lic.name),
      rs.stringOpt(lic.description),
      rs.stringOpt(lic.url)
    )
  }

  def withName(license: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[License] = {
    sql"select ${lic.result.*} from ${License.as(lic)} where LOWER(${lic.name}) = LOWER($license)".map(License(lic)).single.apply
  }
}