/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.{Contributor, Person}
import scalikejdbc._


trait ContributorRepository {
  val contributorRepository: ContributorRepository

  class ContributorRepository {
    private val (ctb, p) = (Contributor.syntax, Person.syntax)

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

    def remove(contributor: Contributor)(implicit session: DBSession = ReadOnlyAutoSession): Unit = {
      val ctb = Contributor.column
      delete
        .from(Contributor)
        .where.eq(ctb.id, contributor.id)
        .toSQL.update().apply()
    }

    def forTranslationId(translationId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Contributor] = {
      select
        .from(Contributor as ctb)
        .innerJoin(Person as p).on(p.id, ctb.personId)
        .where.eq(ctb.translationId, translationId).toSQL
        .map(Contributor(ctb, p)).list().apply()
    }
  }
}
