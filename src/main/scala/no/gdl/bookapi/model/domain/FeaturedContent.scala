/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import org.json4s.DefaultFormats
import scalikejdbc._

case class FeaturedContent(id: Option[Long],
                           revision: Option[Int],
                           language: LanguageTag,
                           title: String,
                           description: String,
                           link: String,
                           imageUrl: String)

object FeaturedContent extends SQLSyntaxSupport[FeaturedContent] {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  override val tableName = "featured_content"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(fc: SyntaxProvider[FeaturedContent])(rs: WrappedResultSet): FeaturedContent = apply(fc.resultName)(rs)

  def apply(fc: ResultName[FeaturedContent])(rs: WrappedResultSet): FeaturedContent = FeaturedContent(
    id = rs.longOpt(fc.id),
    revision = rs.intOpt(fc.revision),
    language = LanguageTag(rs.string(fc.language)),
    title = rs.string(fc.title),
    description = rs.string(fc.description),
    link = rs.string(fc.link),
    imageUrl = rs.string(fc.imageUrl)
  )
}
