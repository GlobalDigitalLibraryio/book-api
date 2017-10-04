/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.EducationalAlignment
import scalikejdbc._


trait EducationalAlignmentRepository {
  val educationalAlignmentRepository: EducationalAlignmentRepository

  class EducationalAlignmentRepository {
    private val ea = EducationalAlignment.syntax

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
}
