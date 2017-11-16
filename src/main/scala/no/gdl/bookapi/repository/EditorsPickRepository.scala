/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.sql.PreparedStatement
import java.time.LocalDate

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain.EditorsPick
import scalikejdbc._


trait EditorsPickRepository {
  val editorsPickRepository: EditorsPickRepository

  class EditorsPickRepository {
    private val ep = EditorsPick.syntax

    def add(editorsPick: EditorsPick)(implicit session: DBSession = AutoSession): EditorsPick = {
      import collection.JavaConverters._

      val ep = EditorsPick.column
      val startRevision = 1

      val translationBinder = ParameterBinder(
        value = editorsPick.translationIds,
        binder = (stmt: PreparedStatement, idx: Int) => stmt.setArray(idx, stmt.getConnection.createArrayOf("bigint", editorsPick.translationIds.asJava.toArray)))

      val id = sql"""
        insert into ${EditorsPick.table} (
          ${ep.revision},
          ${ep.language},
          ${ep.translationIds},
          ${ep.dateChanged})
        values (
         ${startRevision},
         ${editorsPick.language.toString()},
         ${translationBinder},
         ${editorsPick.dateChanged})""".updateAndReturnGeneratedKey().apply()

      editorsPick.copy(id = Some(id), revision = Some(startRevision))
    }

    def lastUpdatedEditorsPick(language: LanguageTag)(implicit session: DBSession = ReadOnlyAutoSession): Option[LocalDate] = {
      select(ep.dateChanged)
        .from(EditorsPick as ep)
        .where.eq(ep.language, language.toString).toSQL
        .map(_.localDate(1)).single.apply()
    }

    def forLanguage(language: LanguageTag)(implicit session: DBSession = ReadOnlyAutoSession): Option[EditorsPick] = {
      select
        .from(EditorsPick as ep)
        .where.eq(ep.language, language.toString).toSQL
        .map(EditorsPick(ep))
        .single().apply()
    }
  }
}
