/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.bookapi.model.domain.Publisher
import io.digitallibrary.bookapi.{IntegrationSuite, TestEnvironment}

class PublisherRepositoryTest extends IntegrationSuite with TestEnvironment {

  override val publisherRepository = new PublisherRepository

  test("that Publisher is added") {
    withRollback { implicit session =>
      val testName = "some-name"

      publisherRepository.add(Publisher(None, None, testName))

      val withName = publisherRepository.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.revision.isDefined should be(true)
      withName.head.name should equal(testName)
    }
  }

  test("that withName returns None when no Publisher with given name") {
    val withName = publisherRepository.withName(s"some-name${System.currentTimeMillis()}")
    withName should be (None)
  }

  test("That withName returns the first occurence of Publisher with that name") {
    withRollback { implicit session =>
      val testName = s"Some-publisher-${System.currentTimeMillis()}"
      val publisherDef = Publisher(None, None, testName)

      val publisher1 = publisherRepository.add(publisherDef)
      val publisher2 = publisherRepository.add(publisherDef)
      val publisher3 = publisherRepository.add(publisherDef)

      val withName = publisherRepository.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.id.get should equal(publisher1.id.get)
    }
  }
}
