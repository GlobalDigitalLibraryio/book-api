/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import org.json4s.Formats
import scalikejdbc._

case class InTranslationFile(id: Option[Long],
                             revision: Option[Int],
                             inTranslationId: Long,
                             fileType: String,
                             originalId: Option[Long],
                             filename: String,
                             crowdinFileId: String,
                             translationStatus: String,
                             etag: Option[String])

object InTranslationFile extends SQLSyntaxSupport[InTranslationFile] {
  implicit val formats: Formats = org.json4s.DefaultFormats
  override val tableName = "in_translation_file"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(f: SyntaxProvider[InTranslationFile])(rs: WrappedResultSet): InTranslationFile =
    apply(f.resultName)(rs)


  def apply(f: ResultName[InTranslationFile])(rs: WrappedResultSet): InTranslationFile = InTranslationFile(
    id = rs.longOpt(f.id),
    revision = rs.intOpt(f.revision),
    inTranslationId = rs.long(f.inTranslationId),
    fileType = rs.string(f.fileType),
    originalId = rs.longOpt(f.originalId),
    filename = rs.string(f.filename),
    crowdinFileId = rs.string(f.crowdinFileId),
    translationStatus = rs.string(f.translationStatus),
    etag = rs.stringOpt(f.etag))
}
