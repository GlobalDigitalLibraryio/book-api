/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.InTranslationFile
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession, insert, select, update}

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
        f.fileType -> file.fileType.toString,
        f.originalId -> file.originalId,
        f.filename -> file.filename,
        f.crowdinFileId -> file.crowdinFileId,
        f.translationStatus -> file.translationStatus.toString,
        f.etag -> file.etag
      ).toSQL
        .updateAndReturnGeneratedKey()
        .apply()

      file.copy(id = Some(id), revision = Some(startRevision))
    }

    def updateInTranslationFile(toUpdate: InTranslationFile)(implicit session: DBSession = AutoSession): InTranslationFile = {
      val f = InTranslationFile.column

      val nextRevision = toUpdate.revision.getOrElse(0) + 1
      val count = update(InTranslationFile).set(
        f.revision -> nextRevision,
        f.inTranslationId -> toUpdate.inTranslationId,
        f.fileType -> toUpdate.fileType.toString,
        f.originalId -> toUpdate.originalId,
        f.filename -> toUpdate.filename,
        f.crowdinFileId -> toUpdate.crowdinFileId,
        f.translationStatus -> toUpdate.translationStatus.toString,
        f.etag -> toUpdate.etag
      ).where
        .eq(f.id, toUpdate.id).and
        .eq(f.revision, toUpdate.revision)
        .toSQL.update().apply()

      if (count != 1) {
        throw new OptimisticLockException()
      } else {
        toUpdate.copy(revision = Some(nextRevision))
      }
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

    def forTranslationWithCrowdinFileId(inTranslationId: Option[Long], crowdinFileId: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[InTranslationFile] = {
      select
        .from(InTranslationFile as f)
        .where
          .eq(f.crowdinFileId, crowdinFileId).and
          .eq(f.inTranslationId, inTranslationId)
        .toSQL
        .map(InTranslationFile(f))
        .single().apply()
    }
  }

}
