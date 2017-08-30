/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.{ignore, _}
import org.json4s.native.Serialization._
import scalikejdbc._


case class Book(id: Option[Long],
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
                bookInLanguage: Seq[BookInLanguage] = Nil)

object Book extends SQLSyntaxSupport[Book] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "book"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(s: SyntaxProvider[Book])(rs: WrappedResultSet): Book = apply(s.resultName)(rs)

  def apply(s: ResultName[Book])(rs: WrappedResultSet): Book = {
    val meta = read[Book](rs.string(s.c("document")))
    Book(Some(rs.long(s.c("id"))), Some(rs.int(s.c("revision"))), rs.stringOpt(s.c("external_id")), meta.title, meta.description, meta.language, meta.license, meta.publisher, meta.readingLevel, meta.typicalAgeRange, meta.educationalUse, meta.educationalRole, meta.timeRequired, meta.categories)
  }

  val JSonSerializer = FieldSerializer[Book](
    ignore("id") orElse
      ignore("revision") orElse
      ignore("bookInLanguage") orElse
      ignore("externalId")
  )
}
