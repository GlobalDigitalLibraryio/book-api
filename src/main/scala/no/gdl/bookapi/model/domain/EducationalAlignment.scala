/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.OptimisticLockException
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
  private val ea = EducationalAlignment.syntax

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

  def add(educationalAlignment: EducationalAlignment)(implicit session: DBSession = AutoSession): EducationalAlignment = {
    val ea = EducationalAlignment.column
    val startRevision = 1

    val id = insert.into(EducationalAlignment).namedValues(
      ea.revision -> startRevision,
      ea.alignmentType -> educationalAlignment.alignmentType,
      ea.educationalFramework -> educationalAlignment.educationalFramework,
      ea.targetDescription -> educationalAlignment.targetDescription,
      ea.targetName -> educationalAlignment.targetName,
      ea.targetUrl -> educationalAlignment.targetUrl
    ).toSQL.updateAndReturnGeneratedKey().apply()

    educationalAlignment.copy(id = Some(id), revision = Some(startRevision))
  }

  def remove(id: Option[Long])(implicit session: DBSession = ReadOnlyAutoSession): Unit = {
    val ea = EducationalAlignment.column
    delete
      .from(EducationalAlignment)
      .where.eq(ea.id, id).toSQL
      .update().apply()
  }

  def updateEducationalAlignment(replacement: EducationalAlignment)(implicit session: DBSession = ReadOnlyAutoSession): EducationalAlignment = {
    val ea = EducationalAlignment.column
    val nextRevision = replacement.revision.getOrElse(0) + 1

    val count = update(EducationalAlignment).set(
      ea.alignmentType -> replacement.alignmentType,
      ea.educationalFramework -> replacement.educationalFramework,
      ea.targetDescription -> replacement.targetDescription,
      ea.targetName -> replacement.targetName,
      ea.targetUrl -> replacement.targetUrl
    ).where
      .eq(ea.id, replacement.id).and
      .eq(ea.revision, replacement.revision).toSQL.update().apply()

    if(count != 1) {
      throw new OptimisticLockException()
    } else {
      replacement.copy(revision = Some(nextRevision))
    }
  }

  def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[EducationalAlignment] = {
    select
      .from(EducationalAlignment as ea)
      .where.eq(ea.id, id).toSQL
      .map(EducationalAlignment(ea)).single().apply()
  }
}