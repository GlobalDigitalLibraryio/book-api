/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import org.json4s.Formats
import scalikejdbc._

case class InTranslation (id: Option[Long],
                          revision: Option[Int],
                          userIds: Seq[String],
                          originalId: Long,
                          newId: Option[Long],
                          fromLanguage: LanguageTag,
                          toLanguage: LanguageTag,
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
    originalId = rs.long(t.originalId),
    newId = rs.longOpt(t.newId),
    fromLanguage = LanguageTag(rs.string(t.fromLanguage)),
    toLanguage = LanguageTag(rs.string(t.toLanguage)),
    crowdinProjectId = rs.string(t.crowdinProjectId))
}
