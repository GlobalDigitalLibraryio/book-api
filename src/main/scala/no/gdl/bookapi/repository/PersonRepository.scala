/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.Person
import scalikejdbc._

trait PersonRepository {
  val personRepository: PersonRepository

  class PersonRepository {
    private val p = Person.syntax
    private val pc = Person.column

    def withGdlId(gdlId: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Person] = {
      select
        .from(Person as p)
        .where
        .eq(p.gdlId, gdlId)
        .toSQL
        .map(Person(p))
        .single.apply()
    }

    def withName(name: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Person] = {
      sql"select ${p.result.*} from ${Person.as(p)} where LOWER(${p.name}) = LOWER($name) order by ${p.id}".map(Person(p)).list.apply.headOption
    }

    def add(person: Person)(implicit session: DBSession = AutoSession): Person = {
      val startRevision = 1

      val id = insert.into(Person).namedValues(
        pc.revision -> startRevision,
        pc.name -> person.name
      ).toSQL.updateAndReturnGeneratedKey().apply()

      person.copy(id = Some(id), revision = Some(startRevision))
    }

    def updatePerson(toUpdate: Person)(implicit session: DBSession = AutoSession): Person = {
      val nextRevision = toUpdate.revision.getOrElse(0) + 1

      val count = update(Person).set(
        pc.revision -> nextRevision,
        pc.name -> toUpdate.name,
        pc.gdlId -> toUpdate.gdlId
      ).where
        .eq(pc.id, toUpdate.id).and
        .eq(pc.revision, toUpdate.revision)
        .toSQL.update().apply()

      if (count != 1) {
        throw new OptimisticLockException()
      } else {
        toUpdate.copy(revision = Some(nextRevision))
      }
    }
  }
}
