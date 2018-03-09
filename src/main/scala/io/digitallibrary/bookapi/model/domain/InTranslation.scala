/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties
import org.json4s.Formats
import scalikejdbc._

case class InTranslation (id: Option[Long],
                          revision: Option[Int],
                          userIds: Seq[String],
                          originalTranslationId: Long,
                          newTranslationId: Option[Long],
                          fromLanguage: LanguageTag,
                          toLanguage: LanguageTag,
                          crowdinToLanguage: String,
                          crowdinProjectId: String)

object InTranslation extends SQLSyntaxSupport[InTranslation] {
  implicit val formats: Formats = org.json4s.DefaultFormats
  override val tableName = "in_translation"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(t: SyntaxProvider[InTranslation])(rs: WrappedResultSet): InTranslation =
    apply(t.resultName)(rs)


  def apply(t: ResultName[InTranslation])(rs: WrappedResultSet): InTranslation = InTranslation(
    id = rs.longOpt(t.id),
    revision = rs.intOpt(t.revision),
    userIds = rs.arrayOpt(t.userIds).map(_.getArray.asInstanceOf[Array[String]].toList).getOrElse(Seq()),
    originalTranslationId = rs.long(t.originalTranslationId),
    newTranslationId = rs.longOpt(t.newTranslationId),
    fromLanguage = LanguageTag(rs.string(t.fromLanguage)),
    toLanguage = LanguageTag(rs.string(t.toLanguage)),
    crowdinToLanguage = rs.string(t.crowdinToLanguage),
    crowdinProjectId = rs.string(t.crowdinProjectId))
}
