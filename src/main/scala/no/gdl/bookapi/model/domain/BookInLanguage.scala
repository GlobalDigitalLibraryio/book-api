/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import java.text.SimpleDateFormat
import java.util.Date

import no.gdl.bookapi.BookApiProperties
import org.json4s.{DefaultFormats, FieldSerializer}
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization.read
import scalikejdbc._

case class BookInLanguage(id: Option[Long],
                          revision: Option[Int],
                          bookId: Option[Long],
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


object BookInLanguage extends SQLSyntaxSupport[BookInLanguage] {
  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  }

  override val tableName = "bookinlanguage"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(s: SyntaxProvider[BookInLanguage])(rs:WrappedResultSet): BookInLanguage = apply(s.resultName)(rs)
  def apply(s: ResultName[BookInLanguage])(rs: WrappedResultSet): BookInLanguage = {
    val meta = read[BookInLanguage](rs.string(s.c("document")))
    BookInLanguage(Some(rs.long(s.c("id"))), Some(rs.int(s.c("revision"))), Some(rs.long(s.c("book_id"))), rs.stringOpt(s.c("external_id")), meta.title, meta.description, meta.language, meta.coverPhoto, meta.downloads,meta.dateCreated, meta.datePublished, meta.tags, meta.authors)
  }

  def opt(bIL: ResultName[BookInLanguage])(rs: WrappedResultSet): Option[BookInLanguage] = rs.longOpt(bIL.c("id")).map(_ => BookInLanguage(bIL)(rs))

  val JSonSerializer = FieldSerializer[Book](
    ignore("id") orElse
      ignore("revision") orElse
      ignore("bookId") orElse
      ignore("externalId")
  )
}
