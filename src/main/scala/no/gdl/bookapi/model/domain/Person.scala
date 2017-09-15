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
  private val p = Person.syntax

  def apply(p: SyntaxProvider[Person])(rs: WrappedResultSet): Person = apply(p.resultName)(rs)
  def apply(p: ResultName[Person])(rs: WrappedResultSet) = new Person(
    rs.longOpt(p.id),
    rs.intOpt(p.revision),
    rs.string(p.name)
  )

  def opt(p: SyntaxProvider[Person])(rs: WrappedResultSet): Option[Person] =
    rs.longOpt(p.resultName.id).map(_ => Person(p)(rs))

  def withName(name: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Person] = {
    sql"select ${p.result.*} from ${Person.as(p)} where LOWER(${p.name}) = LOWER($name)".map(Person(p)).single.apply
  }

  def add(person: Person)(implicit session: DBSession = AutoSession): Person = {
    val p = Person.column
    val startRevision = 1

    val id = insert.into(Person).namedValues(
      p.revision -> startRevision,
      p.name -> person.name
    ).toSQL.updateAndReturnGeneratedKey().apply()

    person.copy(id = Some(id), revision = Some(startRevision))
  }
}