/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}

class PublisherTest extends IntegrationSuite with TestEnvironment {

  test("that Publisher is added") {
    withRollback { implicit session =>
      val testName = "some-name"

      Publisher.add(Publisher(None, None, testName))

      val withName = Publisher.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.revision.isDefined should be(true)
      withName.head.name should equal(testName)
    }
  }

  test("that withName returns None when no Publisher with given name") {
    val withName = Publisher.withName(s"some-name${System.currentTimeMillis()}")
    withName should be (None)
  }

  test("That withName returns the first occurence of Publisher with that name") {
    withRollback { implicit session =>
      val testName = s"Some-publisher-${System.currentTimeMillis()}"
      val publisherDef = Publisher(None, None, testName)

      val publisher1 = Publisher.add(publisherDef)
      val publisher2 = Publisher.add(publisherDef)
      val publisher3 = Publisher.add(publisherDef)

      val withName = Publisher.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.id.get should equal(publisher1.id.get)
    }
  }
}
