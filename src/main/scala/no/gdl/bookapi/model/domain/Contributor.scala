/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._

case class Contributor (id: Option[Long],
                        revision: Option[Int],
                        personId: Long,
                        translationId: Long,
                        `type`: String,
                        person: Person)

object Contributor extends SQLSyntaxSupport[Contributor] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "contributor"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(ctb: SyntaxProvider[Contributor], p: SyntaxProvider[Person])(rs: WrappedResultSet): Contributor = apply(ctb.resultName, p.resultName)(rs)
  def apply(ctb: ResultName[Contributor], p: ResultName[Person])(rs: WrappedResultSet) = new Contributor(
    rs.longOpt(ctb.id),
    rs.intOpt(ctb.revision),
    rs.long(ctb.personId),
    rs.long(ctb.translationId),
    rs.string(ctb.`type`),
    Person.apply(p)(rs)
  )

  def opt(ctb: SyntaxProvider[Contributor], p: SyntaxProvider[Person])(rs: WrappedResultSet): Option[Contributor] =
    rs.longOpt(ctb.resultName.id).map(_ => Contributor(ctb, p)(rs))

  def add(contributor: Contributor)(implicit session: DBSession = AutoSession): Contributor = {
    val ctb = Contributor.column
    val startRevision = 1

    val id = insert.into(Contributor).namedValues(
      ctb.revision -> startRevision,
      ctb.personId -> contributor.person.id.get,
      ctb.translationId -> contributor.translationId,
      ctb.`type` -> contributor.`type`
    ).toSQL.updateAndReturnGeneratedKey().apply()

    contributor.copy(id = Some(id), revision = Some(startRevision))
  }
}

