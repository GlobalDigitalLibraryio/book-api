/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain._
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class ContributorRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val bookRepository = new BookRepository
  override val categoryRepository = new CategoryRepository
  override val contributorRepository = new ContributorRepository
  override val translationRepository = new TranslationRepository
  override val licenseRepository = new LicenseRepository
  override val personRepository = new PersonRepository
  override val publisherRepository = new PublisherRepository

  test("that Contributor.add returns a Contributor with id") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, LanguageTag("eng"))
      val person = personRepository.add(Person(None, None, "Some person", None))

      val contributor = Contributor(None, None, person.id.get, translation.id.get, "Author", person)

      val persisted = contributorRepository.add(contributor)
      persisted.id.isDefined should be(true)
      persisted.revision.isDefined should be(true)
    }

  }

  test("that Contributor.forTranslationId returns all contributors for translation") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, LanguageTag("eng"))
      val person1 = personRepository.add(Person(None, None, "Some person", None))
      val person2 = personRepository.add(Person(None, None, "Some other person", None))

      val contributor1 = Contributor(None, None, person1.id.get, translation.id.get, "Author", person1)
      val contributor2 = Contributor(None, None, person2.id.get, translation.id.get, "Translator", person2)

      val persisted1 = contributorRepository.add(contributor1)
      val persisted2 = contributorRepository.add(contributor2)

      val contributors = contributorRepository.forTranslationId(translation.id.get)
      contributors.length should be(2)
      contributors.minBy(_.id).id should equal(persisted1.id)
      contributors.maxBy(_.id).id should equal(persisted2.id)
    }
  }
}
