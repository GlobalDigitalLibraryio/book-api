/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import java.time.LocalDate
import java.util.UUID

import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.FeedCategory
import no.gdl.bookapi.model.domain.{Category, Chapter, Contributor, EducationalAlignment}

object TestData {
  val LanguageCodeNorwegian = "nob"
  val LanguageCodeEnglish = "eng"
  val LanguageCodeAmharic = "amh"

  val DefaultUuid = UUID.randomUUID().toString
  val DefaultLanguage = LanguageCodeNorwegian
  val Level1 = "1"
  val ageRangeDefault = "5-10"
  val today = LocalDate.now()
  val yesterday = today.minusDays(1)
  val epub = "url-to-epub"
  val pdf = "url-to-pdf"


  object Api {


    val english = api.Language(LanguageCodeEnglish, "English")
    val norwegian_bokmal = api.Language(LanguageCodeNorwegian, "Bokmål, Norwegian")
    val amharic = api.Language(LanguageCodeAmharic, "Amharic")

    val DefaultLicense = api.License(1, 1, "lisens", Some("beskrivelse"), Some("url"))
    val DefaultPublisher = api.Publisher(1, 1, "Some Publisher")

    val category1 = api.Category(1, 1, "category1")
    val category2 = api.Category(2, 1, "category2")

    val DefaultContributor = api.Contributor(1, 1, "type", "contributorname")

    val ChapterSummary1 = api.ChapterSummary(1, 1, Some("Title"), "some-url")
    val Chapter1 = api.Chapter(1, 1, 1, Some("Title"), "Content")

    val DefaultBook = api.Book(
      1, 1, Some("external-1"), DefaultUuid, "Title", "Description", english, Seq(english, norwegian_bokmal), DefaultLicense, DefaultPublisher,
      Some(Level1), Some(ageRangeDefault), None, None, None, Some(today), Some(yesterday), today, Seq(category1, category2), None, api.Downloads(epub, pdf), Seq(), Seq(DefaultContributor), Seq(ChapterSummary1))

    val DefaultFeedDefinition = api.FeedDefinition(1, 1, "some-url", "some-uuid")
    val DefaultFeed = api.Feed(DefaultFeedDefinition, "default title", Some("default description"), Some("default-rel"), yesterday, Seq())
    val DefaultFeedEntry = api.FeedEntry(DefaultBook, Seq())
    val DefaultFeedCategory = FeedCategory("some-url", "some-title", 1)
  }

  object Domain {
    val DefaultPublisher = domain.Publisher(Some(1), Some(1), "Default publisher")
    val DefaultLicense = domain.License(Some(1), Some(1), "Default License", Some("Default license description"), Some("http://some.url.com"))
    val DefaultCategory = domain.Category(Some(1), Some(1), "Default Category")

    val DefaultBook = domain.Book(
      id = Some(1),
      revision = Some(1),
      publisherId = DefaultPublisher.id.get,
      licenseId = DefaultLicense.id.get,
      publisher = DefaultPublisher,
      license = DefaultLicense)

    val DefaultTranslationId = 1
    val DefaultChapter = domain.Chapter(Some(1), Some(1), DefaultTranslationId, 1, Some("Default chapter title"), "Chapter-content")

    val DefaultPerson = domain.Person(Some(1), Some(1), "Default person name")
    val DefaultContributor = domain.Contributor(Some(1), Some(1), DefaultPerson.id.get, DefaultTranslationId, "Author", DefaultPerson)

    val DefaultTranslation: domain.Translation = domain.Translation(
      id = Some(DefaultTranslationId),
      revision = Some(1),
      bookId = DefaultBook.id.get,
      externalId = Some("ext-id"),
      uuid = DefaultUuid,
      title = "Default translation title",
      about = "Default translation description",
      numPages = Some(10),
      language = DefaultLanguage,
      datePublished = Some(today),
      dateCreated = Some(yesterday),
      dateArrived = today,
      categoryIds = Seq(DefaultCategory.id.get),
      coverphoto = None,
      tags = Seq("tag1", "tag2"),
      isBasedOnUrl = None,
      educationalUse = None,
      educationalRole = None,
      eaId = None,
      timeRequired = None,
      typicalAgeRange = None,
      readingLevel = Some("1"),
      interactivityType = None,
      learningResourceType = None,
      accessibilityApi = None,
      accessibilityControl = None,
      accessibilityFeature = None,
      accessibilityHazard = None,
      educationalAlignment = None,
      chapters = Seq(DefaultChapter),
      contributors = Seq(DefaultContributor),
      categories = Seq(DefaultCategory)
    )

  }

}
