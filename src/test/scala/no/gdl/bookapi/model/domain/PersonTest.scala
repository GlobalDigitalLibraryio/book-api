/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class PersonTest extends IntegrationSuite with TestEnvironment {

  test("that Person is added") {
    withRollback { implicit session =>
      val testName = "some-name"

      Person.add(Person(None, None, testName))

      val withName = Person.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.revision.isDefined should be(true)
      withName.head.name should equal(testName)
    }
  }

  test("that withName returns None when no Persons with given name") {
    val withName = Person.withName(s"some-name${System.currentTimeMillis()}")
    withName should be (None)
  }

  test("That withName returns the first occurence of person with that name") {
    withRollback { implicit session =>
      val testName = s"Some-person-${System.currentTimeMillis()}"
      val personDef = Person(None, None, testName)

      val person1 = Person.add(personDef)
      val person2 = Person.add(personDef)
      val person3 = Person.add(personDef)

      val withName = Person.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.id.get should equal(person1.id.get)
    }
  }

}
