/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class ContributorTest extends IntegrationSuite with TestEnvironment {

  test("that Contributor.add returns a Contributor with id") {
    withRollback { implicit session =>
      val book = TranslationTest.addBookDef()
      val translation = TranslationTest.addTranslationDef("external-id", "Some title", book.id.get, "eng")
      val person = Person.add(Person(None, None, "Some person"))

      val contributor = Contributor(None, None, person.id.get, translation.id.get, "Author", person)

      val persisted = Contributor.add(contributor)
      persisted.id.isDefined should be(true)
      persisted.revision.isDefined should be(true)
    }

  }

  test("that Contributor.forTranslationId returns all contributors for translation") {
    withRollback { implicit session =>
      val book = TranslationTest.addBookDef()
      val translation = TranslationTest.addTranslationDef("external-id", "Some title", book.id.get, "eng")
      val person1 = Person.add(Person(None, None, "Some person"))
      val person2 = Person.add(Person(None, None, "Some other person"))

      val contributor1 = Contributor(None, None, person1.id.get, translation.id.get, "Author", person1)
      val contributor2 = Contributor(None, None, person2.id.get, translation.id.get, "Translator", person2)

      val persisted1 = Contributor.add(contributor1)
      val persisted2 = Contributor.add(contributor2)

      val contributors = Contributor.forTranslationId(translation.id.get)
      contributors.length should be(2)
      contributors.minBy(_.id).id should equal(persisted1.id)
      contributors.maxBy(_.id).id should equal(persisted2.id)
    }
  }

}
