/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.model.domain

import no.gdl.readingmaterialsapi.ReadingMaterialsApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization.read
import scalikejdbc._

case class ReadingMaterialInLanguage(id: Option[Long],
                                     revision: Option[Int],
                                     readingMaterialId: Option[Long],
                                     title: String,
                                     description: String,
                                     language: String,
                                     coverPhoto: CoverPhoto,
                                     downloads: Downloads,
                                     tags: Seq[String],
                                     authors: Seq[Author])


object ReadingMaterialInLanguage extends SQLSyntaxSupport[ReadingMaterialInLanguage] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "readingmaterialinlanguage"
  override val schemaName = Some(ReadingMaterialsApiProperties.MetaSchema)

  def apply(s: SyntaxProvider[ReadingMaterialInLanguage])(rs:WrappedResultSet): ReadingMaterialInLanguage = apply(s.resultName)(rs)
  def apply(s: ResultName[ReadingMaterialInLanguage])(rs: WrappedResultSet): ReadingMaterialInLanguage = {
    val meta = read[ReadingMaterialInLanguage](rs.string(s.c("document")))
    ReadingMaterialInLanguage(Some(rs.long(s.c("id"))), Some(rs.int(s.c("revision"))), Some(rs.long(s.c("reading_material_id"))), meta.title, meta.description, meta.language, meta.coverPhoto, meta.downloads, meta.tags, meta.authors)
  }

  def opt(rmIL: ResultName[ReadingMaterialInLanguage])(rs: WrappedResultSet): Option[ReadingMaterialInLanguage] = rs.longOpt(rmIL.c("id")).map(_ => ReadingMaterialInLanguage(rmIL)(rs))

  val JSonSerializer = FieldSerializer[ReadingMaterial](
    ignore("id") orElse
      ignore("revision") orElse
      ignore("readingMaterialId")
  )
}
