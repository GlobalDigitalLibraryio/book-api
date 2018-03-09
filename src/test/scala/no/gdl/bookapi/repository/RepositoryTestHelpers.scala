/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.time.LocalDate
import java.util.UUID

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain._
import scalikejdbc.{AutoSession, DBSession}


trait RepositoryTestHelpers {
  this: BookRepository with PublisherRepository with LicenseRepository with CategoryRepository with TranslationRepository =>

  def addBookDef()(implicit session: DBSession = AutoSession): Book = {
    val publisher = publisherRepository.add(Publisher(None, None, "Publisher Name"))
    val license = licenseRepository.add(License(None, None, "License Name", None, None))

    bookRepository.add(Book(None, None, publisher.id.get, license.id.get, publisher, license, "storyweaver"))
  }

  def addTranslationDef(externalId: String,
                        title: String,
                        bookId: Long,
                        language: LanguageTag,
                        readingLevel: Option[String] = None,
                        dateArrived: Option[LocalDate] = None,
                        status: PublishingStatus.Value = PublishingStatus.PUBLISHED,
                        categoryName: Option[String] = None)(implicit session: DBSession = AutoSession): Translation = {
    val category: Category = categoryName match {
      case Some(name) => categoryRepository.withName(name).getOrElse(categoryRepository.add(Category(None, None, name)))
      case None => categoryRepository.add(Category(None, None, "some-category"))
    }

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
      translatedFrom = None,
      datePublished = Some(LocalDate.now()),
      dateCreated = Some(LocalDate.now()),
      dateArrived = dateArrived.getOrElse(LocalDate.now()),
      publishingStatus = status,
      categoryIds = Seq(category.id.get),
      coverphoto = None,
      tags = Seq("tag1", "tag2"),
      isBasedOnUrl = None,
      educationalUse = None,
      educationalRole = None,
      eaId = None,
      timeRequired = None,
      typicalAgeRange = None,
      readingLevel = readingLevel,
      interactivityType = None,
      learningResourceType = None,
      accessibilityApi = None,
      accessibilityControl = None,
      accessibilityFeature = None,
      accessibilityHazard = None,
      educationalAlignment = None,
      chapters = Seq(),
      contributors = Seq(),
      categories = Seq(category),
      bookFormat = BookFormat.HTML)

    translationRepository.add(translationDef)
  }
}
