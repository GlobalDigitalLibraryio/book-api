/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.Feed
import scalikejdbc._

trait FeedRepository {

  val feedRepository: FeedRepository

  class FeedRepository extends LazyLogging {
    private val f = Feed.syntax

    def forUrl(url: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Feed] = {
      select
        .from(Feed as f)
        .where.eq(f.url, url).toSQL
        .map(Feed(f)).single().apply()
    }

    def addOrUpdate(feed: Feed)(implicit session: DBSession = AutoSession): Feed = {
      val f = Feed.column
      val startRevision = 1

      forUrl(feed.url) match {
        case Some(existingFeed) =>
          val newRevision = existingFeed.revision.getOrElse(0) + 1
          val count = update(Feed).set(
            f.revision -> newRevision,
            f.title -> feed.title,
            f.description -> feed.description
          ).where
            .eq(f.id, existingFeed.id).and
            .eq(f.revision, existingFeed.revision).toSQL.update().apply()
          if (count != 1) {
            throw new OptimisticLockException()
          } else {
            existingFeed.copy(revision = Some(newRevision), title = feed.title, description = feed.description)
          }
        case None =>
          val id = insert.into(Feed).namedValues(
            f.revision -> startRevision,
            f.url -> feed.url,
            f.uuid -> feed.uuid,
            f.title -> feed.title,
            f.description -> feed.description
          ).toSQL.updateAndReturnGeneratedKey().apply()

          feed.copy(id = Some(id), revision = Some(startRevision))
      }

    }

    def all()(implicit session: DBSession = ReadOnlyAutoSession): Seq[Feed] = {
      select
        .from(Feed as f).toSQL
        .map(Feed(f)).list().apply()
    }
  }

}
