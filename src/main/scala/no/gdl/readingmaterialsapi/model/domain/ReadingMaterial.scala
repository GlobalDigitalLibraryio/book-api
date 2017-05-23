/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.model.domain

import no.gdl.readingmaterialsapi.ReadingMaterialsApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.{ignore, _}
import org.json4s.native.Serialization._
import scalikejdbc._


case class ReadingMaterial(id: Option[Long],
                           revision: Option[Int],
                           externalId: Option[String],
                           title: String,
                           description: String,
                           language: String,
                           license: String,
                           publisher: String,
                           readingLevel: Option[String],
                           typicalAgeRange: Option[String],
                           educationalUse: Option[String],
                           educationalRole: Option[String],
                           timeRequired: Option[String],
                           categories: Seq[String],
                           readingMaterialInLanguage: Seq[ReadingMaterialInLanguage] = Nil)

object ReadingMaterial extends SQLSyntaxSupport[ReadingMaterial] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "readingmaterial"
  override val schemaName = Some(ReadingMaterialsApiProperties.MetaSchema)

  def apply(s: SyntaxProvider[ReadingMaterial])(rs: WrappedResultSet): ReadingMaterial = apply(s.resultName)(rs)

  def apply(s: ResultName[ReadingMaterial])(rs: WrappedResultSet): ReadingMaterial = {
    val meta = read[ReadingMaterial](rs.string(s.c("document")))
    ReadingMaterial(Some(rs.long(s.c("id"))), Some(rs.int(s.c("revision"))), rs.stringOpt(s.c("external_id")), meta.title, meta.description, meta.language, meta.license, meta.publisher, meta.readingLevel, meta.typicalAgeRange, meta.educationalUse, meta.educationalRole, meta.timeRequired, meta.categories)
  }

  val JSonSerializer = FieldSerializer[ReadingMaterial](
    ignore("id") orElse
      ignore("revision") orElse
      ignore("readingMaterialInLanguage") orElse
      ignore("externalId")
  )
}
