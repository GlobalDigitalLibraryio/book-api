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
                             fileType: FileType.Value,
                             originalId: Option[Long],
                             filename: String,
                             crowdinFileId: String,
                             translationStatus: TranslationStatus.Value,
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
    fileType = FileType.valueOf(rs.string(f.fileType)).get,
    originalId = rs.longOpt(f.originalId),
    filename = rs.string(f.filename),
    crowdinFileId = rs.string(f.crowdinFileId),
    translationStatus = TranslationStatus.valueOf(rs.string(f.translationStatus)).get,
    etag = rs.stringOpt(f.etag))
}

object TranslationStatus extends Enumeration {
  val IN_PROGRESS, TRANSLATED = Value

  def valueOf(s: String): Option[TranslationStatus.Value] = {
    TranslationStatus.values.find(_.toString == s.toUpperCase)
  }
}

object FileType extends Enumeration {
  val METADATA, CONTENT = Value

  def valueOf(s: String): Option[FileType.Value] = {
    FileType.values.find(_.toString == s.toUpperCase)
  }
}
