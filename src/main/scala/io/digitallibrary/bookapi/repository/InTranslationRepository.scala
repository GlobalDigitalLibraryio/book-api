/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import java.sql.PreparedStatement

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api.OptimisticLockException
import io.digitallibrary.bookapi.model.domain.InTranslation
import scalikejdbc._

trait InTranslationRepository {
  val inTranslationRepository: InTranslationRepository

  class InTranslationRepository {


    private val tr = InTranslation.syntax

    def add(inTranslation: InTranslation)(implicit session: DBSession = AutoSession): InTranslation = {
      import collection.JavaConverters._

      val t = InTranslation.column
      val startRevision = 1

      val userBinder = ParameterBinder(
        value = inTranslation.userIds,
        binder = (stmt: PreparedStatement, idx: Int) => stmt.setArray(idx, stmt.getConnection.createArrayOf("text", inTranslation.userIds.asJava.toArray))
      )

      val id = insert.into(InTranslation).namedValues(
        t.revision -> startRevision,
        t.userIds -> userBinder,
        t.originalTranslationId -> inTranslation.originalTranslationId,
        t.newTranslationId -> inTranslation.newTranslationId,
        t.fromLanguage -> inTranslation.fromLanguage.toString,
        t.toLanguage -> inTranslation.toLanguage.toString,
        t.crowdinToLanguage -> inTranslation.crowdinToLanguage,
        t.crowdinProjectId -> inTranslation.crowdinProjectId
      ).toSQL
        .updateAndReturnGeneratedKey()
        .apply()

      inTranslation.copy(id = Some(id), revision = Some(startRevision))
    }

    def updateTranslation(toUpdate: InTranslation)(implicit session: DBSession = AutoSession): InTranslation = {
      import collection.JavaConverters._
      val t = InTranslation.column
      val nextRevision = toUpdate.revision.getOrElse(0) + 1

      val userBinder = ParameterBinder(
        value = toUpdate.userIds,
        binder = (stmt: PreparedStatement, idx: Int) => stmt.setArray(idx, stmt.getConnection.createArrayOf("text", toUpdate.userIds.asJava.toArray))
      )

      val count = update(InTranslation).set(
        t.revision -> nextRevision,
        t.userIds -> userBinder,
        t.originalTranslationId -> toUpdate.originalTranslationId,
        t.newTranslationId -> toUpdate.newTranslationId,
        t.fromLanguage -> toUpdate.fromLanguage.toString,
        t.toLanguage -> toUpdate.toLanguage.toString,
        t.crowdinToLanguage -> toUpdate.crowdinToLanguage,
        t.crowdinProjectId -> toUpdate.crowdinProjectId
      ).where
        .eq(t.id, toUpdate.id).and
        .eq(t.revision, toUpdate.revision)
        .toSQL.update().apply()

      if (count != 1) {
        throw new OptimisticLockException()
      } else {
        toUpdate.copy(revision = Some(nextRevision))
      }
    }

    def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[InTranslation] = {
      select
        .from(InTranslation as tr)
        .where.eq(tr.id, id)
        .toSQL
        .map(InTranslation(tr))
        .single().apply()
    }

    def forOriginalIdWithToLanguage(originalTranslationId: Long, toLanguage: LanguageTag)(implicit session: DBSession = ReadOnlyAutoSession): Option[InTranslation] = {
      select
        .from(InTranslation as tr)
        .where.eq(tr.originalTranslationId, originalTranslationId)
        .and.eq(tr.toLanguage, toLanguage.toString)
        .toSQL
        .map(InTranslation(tr))
        .single().apply()
    }

    def forOriginalId(originalTranslationId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[InTranslation] = {
      select
        .from(InTranslation as tr)
        .where.eq(tr.originalTranslationId, originalTranslationId)
        .toSQL
        .map(InTranslation(tr))
        .list().apply()
    }

    def inTranslationForUser(userId: String)(implicit session: DBSession = ReadOnlyAutoSession): Seq[InTranslation] = {
      sql"select ${tr.result.*} from ${InTranslation.as(tr)} where $userId = ANY(user_ids)".map(InTranslation(tr)).list().apply()
    }
  }

}
