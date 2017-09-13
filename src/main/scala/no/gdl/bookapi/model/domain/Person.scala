/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._


case class Person(id: Option[Long],
             revision: Option[Int],
             name: String)

object Person extends SQLSyntaxSupport[Person] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "person"
  override val schemaName = Some(BookApiProperties.MetaSchema)
  override val columns = Seq("id", "revision", "name")

  def apply(p: SyntaxProvider[Person])(rs: WrappedResultSet): Person = apply(p.resultName)(rs)
  def apply(p: ResultName[Person])(rs: WrappedResultSet) = new Person(
    rs.longOpt(p.id),
    rs.intOpt(p.revision),
    rs.string(p.name)
  )

  def opt(p: SyntaxProvider[Person])(rs: WrappedResultSet): Option[Person] =
    rs.longOpt(p.resultName.id).map(_ => Person(p)(rs))
}