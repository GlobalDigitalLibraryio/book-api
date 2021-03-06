/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

import io.digitallibrary.bookapi.BookApiProperties
import org.json4s.Formats
import scalikejdbc._

case class InTranslationFile(id: Option[Long],
                             revision: Option[Int],
                             inTranslationId: Long,
                             fileType: FileType.Value,
                             newChapterId: Option[Long],
                             seqNo: Int,
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
    newChapterId = rs.longOpt(f.newChapterId),
    seqNo = rs.int(f.seqNo),
    filename = rs.string(f.filename),
    crowdinFileId = rs.string(f.crowdinFileId),
    translationStatus = TranslationStatus.valueOf(rs.string(f.translationStatus)).get,
    etag = rs.stringOpt(f.etag))
}

object TranslationStatus extends Enumeration {
  val PSEUDO: TranslationStatus.Value = Value(-1)
  val IN_PROGRESS: TranslationStatus.Value = Value(0)
  val TRANSLATED: TranslationStatus.Value = Value(1)
  val PROOFREAD: TranslationStatus.Value = Value(2)

  def valueOf(s: String): Option[TranslationStatus.Value] = {
    TranslationStatus.values.find(_.toString == s.toUpperCase)
  }

  def valueOf(s: Option[String]): Option[TranslationStatus.Value] = {
    s.flatMap(valueOf)
  }
}

object FileType extends Enumeration {
  val METADATA, CONTENT = Value

  def valueOf(s: String): Option[FileType.Value] = {
    FileType.values.find(_.toString == s.toUpperCase)
  }
}
