/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.{Book, License, Publisher}
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class BookRepositoryTest extends IntegrationSuite with TestEnvironment {

  override val bookRepository = new BookRepository
  override val licenseRepository = new LicenseRepository
  override val publisherRepository = new PublisherRepository

  test("that Book is added and retrieved") {
    withRollback { implicit session =>
      val testName = "some-name"
      val publisher = publisherRepository.add(Publisher(None, None, "Publisher Name"))
      val license = licenseRepository.add(License(None, None, "License Name", None, None))

      val book = bookRepository.add(Book(None, None, publisher.id.get, license.id.get, publisher, license, "storyweaver"))

      val withId = bookRepository.withId(book.id.get)
      withId.head.id should equal (book.id)
      withId.head.license.id should equal (license.id)
      withId.head.publisher.id should equal (publisher.id)
    }
  }

  test("that None is returned when id does not exist") {
    bookRepository.withId(100) should equal (None)
  }

}
