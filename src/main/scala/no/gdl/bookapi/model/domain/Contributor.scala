/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class Contributor (id: Option[Long],
                        revision: Option[Int],
                        personId: Long,
                        translationId: Long,
                        `type`: ContributorType.Value,
                        person: Person)

object ContributorType extends Enumeration {
  val Author, Illustrator, Translator, Photographer, Contributor = Value

  def valueOf(s: String): Try[ContributorType.Value] = {
    ContributorType.values.find(_.toString.equalsIgnoreCase(s)) match {
      case Some(x) => Success(x)
      case None => Failure(new RuntimeException(s"Unknown ContributorType $s."))
    }
  }
}

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
    ContributorType.valueOf(rs.string(ctb.`type`)).get,
    Person.apply(p)(rs)
  )

  def opt(ctb: SyntaxProvider[Contributor], p: SyntaxProvider[Person])(rs: WrappedResultSet): Option[Contributor] =
    rs.longOpt(ctb.resultName.id).map(_ => Contributor(ctb, p)(rs))
}

