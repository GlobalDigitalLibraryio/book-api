/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.Publisher
import scalikejdbc._


trait PublisherRepository {
  val publisherRepository: PublisherRepository

  class PublisherRepository {
    private val pub = Publisher.syntax

    def withName(publisher: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Publisher] = {
      sql"select ${pub.result.*} from ${Publisher.as(pub)} where LOWER(${pub.name}) = LOWER($publisher) order by ${pub.id}".map(Publisher(pub)).list.apply.headOption
    }

    def add(publisher: Publisher)(implicit session: DBSession = AutoSession): Publisher = {
      val p = Publisher.column
      val startRevision = 1

      val id = insert.into(Publisher).namedValues(
        p.revision -> startRevision,
        p.name -> publisher.name
      ).toSQL
        .updateAndReturnGeneratedKey()
        .apply()

      publisher.copy(id = Some(id), revision = Some(startRevision))
    }
  }
}
