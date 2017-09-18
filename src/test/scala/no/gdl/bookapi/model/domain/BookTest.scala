/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}

import scala.util.{Failure, Success}


class BookTest extends IntegrationSuite with TestEnvironment {

  test("that Book is added and retrieved") {
    withRollback { implicit session =>
      val testName = "some-name"
      val publisher = Publisher.add(Publisher(None, None, "Publisher Name"))
      val license = License.add(License(None, None, "License Name", None, None))

      val bookTry = Book.add(Book(None, None, publisher.id.get, license.id.get, publisher, license))

      bookTry match {
        case Failure(ex) => fail(ex)
        case Success(book) =>
          val withId = Book.withId(book.id.get)
          withId.head.id should equal (book.id)
          withId.head.license.id should equal (license.id)
          withId.head.publisher.id should equal (publisher.id)
      }
    }
  }

  test("that None is returned when id does not exist") {
    Book.withId(100) should equal (None)
  }

}
