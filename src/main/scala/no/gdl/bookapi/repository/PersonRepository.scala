/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.Person
import scalikejdbc._


trait PersonRepository {
  val personRepository: PersonRepository

  class PersonRepository {
    private val p = Person.syntax

    def withName(name: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Person] = {
      sql"select ${p.result.*} from ${Person.as(p)} where LOWER(${p.name}) = LOWER($name) order by ${p.id}".map(Person(p)).list.apply.headOption
    }

    def add(person: Person)(implicit session: DBSession = AutoSession): Person = {
      val p = Person.column
      val startRevision = 1

      val id = insert.into(Person).namedValues(
        p.revision -> startRevision,
        p.name -> person.name
      ).toSQL.updateAndReturnGeneratedKey().apply()

      person.copy(id = Some(id), revision = Some(startRevision))
    }
  }
}
