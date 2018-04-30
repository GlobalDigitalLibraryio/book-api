/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.bookapi.model.domain.Category
import scalikejdbc._


trait CategoryRepository {
  val categoryRepository: CategoryRepository

  class CategoryRepository {
    private val cat = Category.syntax

    def withName(category: String)(implicit session: DBSession = AutoSession): Option[Category] = {
      sql"select ${cat.result.*} from ${Category.as(cat)} where LOWER(${cat.name}) = LOWER($category) order by ${cat.id}"
        .map(Category(cat))
        .list()
        .apply.headOption
    }

    def withId(id: Long)(implicit session: DBSession = AutoSession): Option[Category] = {
      select
        .from(Category as cat)
        .where.eq(cat.id, id).toSQL
        .map(Category(cat))
        .single()
        .apply()
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

}
