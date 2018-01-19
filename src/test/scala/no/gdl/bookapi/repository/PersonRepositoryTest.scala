/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.Person
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class PersonRepositoryTest extends IntegrationSuite with TestEnvironment {

  override val personRepository = new PersonRepository

  test("that Person is added") {
    withRollback { implicit session =>
      val testName = "some-name"

      personRepository.add(Person(None, None, testName, None))

      val withName = personRepository.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.revision.isDefined should be(true)
      withName.head.name should equal(testName)
    }
  }

  test("that withName returns None when no Persons with given name") {
    val withName = personRepository.withName(s"some-name${System.currentTimeMillis()}")
    withName should be (None)
  }

  test("That withName returns the first occurence of person with that name") {
    withRollback { implicit session =>
      val testName = s"Some-person-${System.currentTimeMillis()}"
      val personDef = Person(None, None, testName, None)

      val person1 = personRepository.add(personDef)
      val person2 = personRepository.add(personDef)
      val person3 = personRepository.add(personDef)

      val withName = personRepository.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.id.get should equal(person1.id.get)
    }
  }

}
