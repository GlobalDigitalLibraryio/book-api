/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.api
import no.gdl.bookapi.model.api.{FeaturedContentId, OptimisticLockException}
import no.gdl.bookapi.model.domain.FeaturedContent
import scalikejdbc._

import scala.util.{Success, Try}


trait FeaturedContentRepository {
  val featuredContentRepository: FeaturedContentRepository

  class FeaturedContentRepository {

    def updateContent(featuredContent: api.FeaturedContent)(implicit session: DBSession = AutoSession): Try[FeaturedContentId] = {
      val f = FeaturedContent.column
      val newRevision = featuredContent.revision + 1
      Try {
        val affectedRows = update(FeaturedContent).set(
          f.revision -> newRevision,
          f.language -> featuredContent.language.code,
          f.title -> featuredContent.title,
          f.description -> featuredContent.description,
          f.link -> featuredContent.link,
          f.imageUrl -> featuredContent.imageUrl)
          .where.eq(f.id, featuredContent.id)
          .toSQL.update().apply()

        if (affectedRows != 1) {
          throw new OptimisticLockException()
        } else {
          FeaturedContentId(featuredContent.id)
        }
      }
    }

    def deleteContent(id: Long)(implicit session: DBSession = AutoSession): Try[Unit] = {
      val affectedRows = withSQL {
        delete
          .from(FeaturedContent as f)
          .where.eq(f.id, id)
      }
        .update()
        .apply()
      if (affectedRows != 1) {
        throw new OptimisticLockException()
      } else {
        Success()
      }
    }

    private val f = FeaturedContent.syntax

    def addContent(featuredContent: FeaturedContent)(implicit session: DBSession = AutoSession): FeaturedContent = {

      val f = FeaturedContent.column
      val startRevision = 1

      val id =
        insert
        .into(FeaturedContent)
        .namedValues(
          f.revision -> startRevision,
          f.language -> featuredContent.language.toString,
          f.title -> featuredContent.title,
          f.description -> featuredContent.description,
          f.link -> featuredContent.link,
          f.imageUrl -> featuredContent.imageUrl
        ).toSQL.updateAndReturnGeneratedKey().apply()

      featuredContent.copy(id = Some(id), revision = Some(startRevision))
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
