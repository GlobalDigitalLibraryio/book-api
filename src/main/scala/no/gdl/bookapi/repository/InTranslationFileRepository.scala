/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.{InTranslation, InTranslationFile}
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession, insert, select, update}

trait InTranslationFileRepository {
  val inTranslationFileRepository: InTranslationFileRepository

  class InTranslationFileRepository {

    private val itf = InTranslationFile.syntax
    private val it = InTranslation.syntax

    def add(file: InTranslationFile)(implicit session: DBSession = AutoSession): InTranslationFile = {
      val col = InTranslationFile.column

      val startRevision = 1
      val id = insert.into(InTranslationFile).namedValues(
        col.revision -> startRevision,
        col.inTranslationId -> file.inTranslationId,
        col.fileType -> file.fileType.toString,
        col.originalId -> file.originalId,
        col.filename -> file.filename,
        col.crowdinFileId -> file.crowdinFileId,
        col.translationStatus -> file.translationStatus.toString,
        col.etag -> file.etag
      ).toSQL
        .updateAndReturnGeneratedKey()
        .apply()

      file.copy(id = Some(id), revision = Some(startRevision))
    }

    def updateInTranslationFile(toUpdate: InTranslationFile)(implicit session: DBSession = AutoSession): InTranslationFile = {
      val col = InTranslationFile.column

      val nextRevision = toUpdate.revision.getOrElse(0) + 1
      val count = update(InTranslationFile).set(
        col.revision -> nextRevision,
        col.inTranslationId -> toUpdate.inTranslationId,
        col.fileType -> toUpdate.fileType.toString,
        col.originalId -> toUpdate.originalId,
        col.filename -> toUpdate.filename,
        col.crowdinFileId -> toUpdate.crowdinFileId,
        col.translationStatus -> toUpdate.translationStatus.toString,
        col.etag -> toUpdate.etag
      ).where
        .eq(col.id, toUpdate.id).and
        .eq(col.revision, toUpdate.revision)
        .toSQL.update().apply()

      if (count != 1) {
        throw new OptimisticLockException()
      } else {
        toUpdate.copy(revision = Some(nextRevision))
      }
    }

    def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[InTranslationFile] = {
      select
        .from(InTranslationFile as itf)
        .where.eq(itf.id, id)
        .toSQL
        .map(InTranslationFile(itf))
        .single().apply()
    }

    def withTranslationId(id: Option[Long])(implicit session: DBSession = ReadOnlyAutoSession): Seq[InTranslationFile] = {
      select
        .from(InTranslationFile as itf)
        .where.eq(itf.inTranslationId, id)
        .toSQL.map(InTranslationFile(itf))
        .list().apply()
    }

    def forCrowdinProjectWithFileIdAndLanguage(crowdinProjectId: String, crowdinFileId: String, language: LanguageTag)(implicit session: DBSession = ReadOnlyAutoSession): Option[InTranslationFile] = {
      select
        .from(InTranslationFile as itf)
        .leftJoin(InTranslation as it)
          .on(it.id, itf.inTranslationId)
        .where
          .eq(itf.crowdinFileId, crowdinFileId).and
          .eq(it.toLanguage, language.toString).and
          .eq(it.crowdinProjectId, crowdinProjectId)
        .toSQL
        .map(InTranslationFile(itf))
        .single().apply()

    }
  }

}