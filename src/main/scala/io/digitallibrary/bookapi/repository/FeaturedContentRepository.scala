/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.model.api.{FeaturedContentId, OptimisticLockException}
import io.digitallibrary.bookapi.model.domain.FeaturedContent
import scalikejdbc._

import scala.util.{Success, Try}


trait FeaturedContentRepository {
  val featuredContentRepository: FeaturedContentRepository

  class FeaturedContentRepository {

    def updateContent(fc: api.FeaturedContent)(implicit session: DBSession = AutoSession): Try[FeaturedContentId] = {
      val f = FeaturedContent.column
      val newRevision = fc.revision + 1

      Try {
        val affectedRows = update(FeaturedContent).set(
          f.revision -> newRevision,
          f.language -> fc.language.code,
          f.title -> fc.title,
          f.description -> fc.description,
          f.link -> fc.link,
          f.imageUrl -> fc.imageUrl)
          .where.eq(f.id, fc.id)
          .toSQL.update().apply()

        if (affectedRows != 1) {
          throw new OptimisticLockException()
        } else {
          FeaturedContentId(fc.id)
        }
      }
    }

    def deleteContent(id: Long)(implicit session: DBSession = AutoSession): Try[Unit] = {
      val affectedRows = delete
        .from(FeaturedContent as f)
        .where.eq(f.id, id)
        .toSQL
        .update()
        .apply()

      if (affectedRows != 1) {
        throw new OptimisticLockException()
      } else {
        Success()
      }
    }

    private val f = FeaturedContent.syntax

    def addContent(fc: FeaturedContent)(implicit session: DBSession = AutoSession): FeaturedContent = {

      val f = FeaturedContent.column
      val startRevision = 1

      val id = insert
        .into(FeaturedContent)
        .namedValues(
          f.revision -> startRevision,
          f.language -> fc.language.toString,
          f.title -> fc.title,
          f.description -> fc.description,
          f.link -> fc.link,
          f.imageUrl -> fc.imageUrl
        ).toSQL.updateAndReturnGeneratedKey().apply()

      fc.copy(id = Some(id), revision = Some(startRevision))
    }

    def forLanguage(language: LanguageTag)(implicit session: DBSession = ReadOnlyAutoSession): Seq[FeaturedContent] = {
      select
        .from(FeaturedContent as f)
        .where.eq(f.language, language.toString).toSQL
        .map(FeaturedContent(f))
        .list()
        .apply()
    }
  }

}
