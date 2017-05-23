/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.model.domain

import java.text.SimpleDateFormat
import java.util.Date

import no.gdl.readingmaterialsapi.ReadingMaterialsApiProperties
import org.json4s.{DefaultFormats, FieldSerializer}
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization.read
import scalikejdbc._

case class ReadingMaterialInLanguage(id: Option[Long],
                                     revision: Option[Int],
                                     readingMaterialId: Option[Long],
                                     externalId: Option[String],
                                     title: String,
                                     description: String,
                                     language: String,
                                     coverPhoto: CoverPhoto,
                                     downloads: Downloads,
                                     dateCreated: Option[Date],
                                     datePublished: Option[Date],
                                     tags: Seq[String],
                                     authors: Seq[String])


object ReadingMaterialInLanguage extends SQLSyntaxSupport[ReadingMaterialInLanguage] {
  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  }

  override val tableName = "readingmaterialinlanguage"
  override val schemaName = Some(ReadingMaterialsApiProperties.MetaSchema)

  def apply(s: SyntaxProvider[ReadingMaterialInLanguage])(rs:WrappedResultSet): ReadingMaterialInLanguage = apply(s.resultName)(rs)
  def apply(s: ResultName[ReadingMaterialInLanguage])(rs: WrappedResultSet): ReadingMaterialInLanguage = {
    val meta = read[ReadingMaterialInLanguage](rs.string(s.c("document")))
    ReadingMaterialInLanguage(Some(rs.long(s.c("id"))), Some(rs.int(s.c("revision"))), Some(rs.long(s.c("reading_material_id"))), rs.stringOpt(s.c("external_id")), meta.title, meta.description, meta.language, meta.coverPhoto, meta.downloads,meta.dateCreated, meta.datePublished, meta.tags, meta.authors)
  }

  def opt(rmIL: ResultName[ReadingMaterialInLanguage])(rs: WrappedResultSet): Option[ReadingMaterialInLanguage] = rs.longOpt(rmIL.c("id")).map(_ => ReadingMaterialInLanguage(rmIL)(rs))

  val JSonSerializer = FieldSerializer[ReadingMaterial](
    ignore("id") orElse
      ignore("revision") orElse
      ignore("readingMaterialId") orElse
      ignore("externalId")
  )
}
