/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import com.typesafe.scalalogging.LazyLogging
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

    def add(feed: Feed)(implicit session: DBSession = AutoSession): Feed = {
      val f = Feed.column
      val startRevision = 1

      val id = insert.into(Feed).namedValues(
        f.revision -> startRevision,
        f.url -> feed.url,
        f.uuid -> feed.uuid,
        f.titleKey -> feed.titleKey,
        f.descriptionKey -> feed.descriptionKey
      ).toSQL.updateAndReturnGeneratedKey().apply()

      feed.copy(id = Some(id), revision = Some(startRevision))
    }

    def all()(implicit session: DBSession = ReadOnlyAutoSession): Seq[Feed] = {
      select
        .from(Feed as f).toSQL
        .map(Feed(f)).list().apply()
    }
  }

}
