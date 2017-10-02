/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.time.LocalDate
import java.util.UUID

import no.gdl.bookapi.model.domain._
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}
import scalikejdbc.{AutoSession, DBSession}


class ContributorRepositoryTest extends IntegrationSuite with TestEnvironment {

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
      val translation = addTranslationDef("external-id", "Some title", book.id.get, "eng")
      val person = personRepository.add(Person(None, None, "Some person"))

      val contributor = Contributor(None, None, person.id.get, translation.id.get, "Author", person)

      val persisted = contributorRepository.add(contributor)
      persisted.id.isDefined should be(true)
      persisted.revision.isDefined should be(true)
    }

  }

  test("that Contributor.forTranslationId returns all contributors for translation") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, "eng")
      val person1 = personRepository.add(Person(None, None, "Some person"))
      val person2 = personRepository.add(Person(None, None, "Some other person"))

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

  def addBookDef()(implicit session: DBSession = AutoSession): Book = {
    val publisher = publisherRepository.add(Publisher(None, None, "Publisher Name"))
    val license = licenseRepository.add(License(None, None, "License Name", None, None))

    bookRepository.add(Book(None, None, publisher.id.get, license.id.get, publisher, license))
  }

  def addTranslationDef(externalId: String, title: String, bookId: Long, language: String)(implicit session: DBSession = AutoSession): Translation = {
    val cat1 = categoryRepository.add(Category(None, None, "some-category"))

    val translationDef = Translation(
      id = None,
      revision = None,
      bookId = bookId,
      externalId = Some(externalId),
      uuid = UUID.randomUUID().toString,
      title = title,
      about = "Some description",
      numPages = Some(123),
      language = language,
      datePublished = Some(LocalDate.now()),
      dateCreated = Some(LocalDate.now()),
      categoryIds = Seq(cat1.id.get),
      coverphoto = None,
      tags = Seq("tag1", "tag2"),
      isBasedOnUrl = None,
      educationalUse = None,
      educationalRole = None,
      eaId = None,
      timeRequired = None,
      typicalAgeRange = None,
      readingLevel = None,
      interactivityType = None,
      learningResourceType = None,
      accessibilityApi = None,
      accessibilityControl = None,
      accessibilityFeature = None,
      accessibilityHazard = None,
      educationalAlignment = None,
      chapters = Seq(),
      contributors = Seq(),
      categories = Seq(cat1)
    )

    translationRepository.add(translationDef)
  }

}
