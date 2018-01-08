/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.InTranslationFile
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession, insert, select}

trait InTranslationFileRepository {
  val inTranslationFileRepository: InTranslationFileRepository

  class InTranslationFileRepository {
    private val f = InTranslationFile.syntax

    def add(file: InTranslationFile)(implicit session: DBSession = AutoSession): InTranslationFile = {
      val f = InTranslationFile.column

      val startRevision = 1
      val id = insert.into(InTranslationFile).namedValues(
        f.revision -> startRevision,
        f.inTranslationId -> file.inTranslationId,
        f.fileType -> file.fileType,
        f.originalId -> file.originalId,
        f.filename -> file.filename,
        f.crowdinFileId -> file.crowdinFileId,
        f.translationStatus -> file.translationStatus,
        f.etag -> file.etag
      ).toSQL
        .updateAndReturnGeneratedKey()
        .apply()

      file.copy(id = Some(id), revision = Some(startRevision))
    }

    def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[InTranslationFile] = {
      select
        .from(InTranslationFile as f)
        .where.eq(f.id, id)
        .toSQL
        .map(InTranslationFile(f))
        .single().apply()
    }

    def withTranslationId(id: Option[Long])(implicit session: DBSession = ReadOnlyAutoSession): Seq[InTranslationFile] = {
      select
        .from(InTranslationFile as f)
        .where.eq(f.inTranslationId, id)
        .toSQL.map(InTranslationFile(f))
        .list().apply()
    }
  }
}
