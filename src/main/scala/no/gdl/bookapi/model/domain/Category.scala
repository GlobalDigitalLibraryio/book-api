/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._

case class Category (id: Option[Long],
                     revision: Option[Int],
                     name: String)

object Category extends SQLSyntaxSupport[Category] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "category"
  override val schemaName = Some(BookApiProperties.MetaSchema)
  override val columns = Seq("id", "revision", "name")
  private val cat = Category.syntax

  def apply(cat: SyntaxProvider[Category])(rs: WrappedResultSet): Category = apply(cat.resultName)(rs)
  def apply(cat: ResultName[Category])(rs: WrappedResultSet) = new Category(
    rs.longOpt(cat.id),
    rs.intOpt(cat.revision),
    rs.string(cat.name)
  )

  def opt(cat: SyntaxProvider[Category])(rs: WrappedResultSet): Option[Category] =
    rs.longOpt(cat.resultName.id).map(_ => Category(cat)(rs))

  def withName(category: String)(implicit session: DBSession = AutoSession): Option[Category] = {
    sql"select ${cat.result.*} from ${Category.as(cat)} where LOWER(${cat.name}) = LOWER($category) order by ${cat.id}"
      .map(Category(cat))
      .list()
      .apply.headOption
  }

  def add(newCategory: Category)(implicit session: DBSession = AutoSession): Category = {
    val c = Category.column
    val startRevision = 1

    val id = insert.into(Category).namedValues(
      c.revision -> startRevision,
      c.name -> newCategory.name
    ).toSQL.updateAndReturnGeneratedKey().apply()

    newCategory.copy(id = Some(id), revision = Some(startRevision))
  }
}