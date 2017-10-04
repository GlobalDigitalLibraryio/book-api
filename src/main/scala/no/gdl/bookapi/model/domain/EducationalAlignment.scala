/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._

case class EducationalAlignment (id: Option[Long],
                                 revision: Option[Int],
                                 alignmentType: Option[String],
                                 educationalFramework: Option[String],
                                 targetDescription: Option[String],
                                 targetName: Option[String],
                                 targetUrl: Option[String])

object EducationalAlignment extends SQLSyntaxSupport[EducationalAlignment] {

  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "educational_alignment"
  override val schemaName = Some(BookApiProperties.MetaSchema)


  def apply(ea: SyntaxProvider[EducationalAlignment])(rs: WrappedResultSet): EducationalAlignment = apply(ea.resultName)(rs)
  def apply(ea: ResultName[EducationalAlignment])(rs: WrappedResultSet): EducationalAlignment =
    EducationalAlignment(
      rs.longOpt(ea.id),
      rs.intOpt(ea.revision),
      rs.stringOpt(ea.alignmentType),
      rs.stringOpt(ea.educationalFramework),
      rs.stringOpt(ea.targetDescription),
      rs.stringOpt(ea.targetName),
      rs.stringOpt(ea.targetUrl))

  def opt(ea: SyntaxProvider[EducationalAlignment])(rs: WrappedResultSet): Option[EducationalAlignment] =
    rs.longOpt(ea.resultName.id).map(_ => EducationalAlignment(ea)(rs))

}