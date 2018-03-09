/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.crowdin.{BookMetaData, TranslatedChapter}
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.Category
import no.gdl.bookapi.model.api.internal.NewTranslatedChapter
import no.gdl.bookapi.model.domain.{BookFormat, ChapterType, ContributorType}

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
    val DefaultSource = "storyweaver"

    val category1 = api.Category(1, 1, "category1")
    val category2 = api.Category(2, 1, "category2")

    val DefaultContributor = api.Contributor(1, 1, "type", "contributorname")

    val ChapterSummary1 = api.ChapterSummary(1, 1, Some("Title"), "some-url")
    val Chapter1 = api.Chapter(1, 1, 1, Some("Title"), "Content")

    val DefaultBook = api.Book(
      1, 1, Some("external-1"), DefaultUuid, "Title", "Description", None, english, Seq(english, norwegian_bokmal), DefaultLicense, DefaultPublisher,
      Some(Level1), Some(ageRangeDefault), None, None, None, Some(today), Some(yesterday), today, Seq(category1, category2), None, api.Downloads(Some(epub), None),
      Seq(), Seq(DefaultContributor), Seq(ChapterSummary1), supportsTranslation = true, bookFormat = BookFormat.HTML.toString, DefaultSource)

    val DefaultBookHit = api.BookHit(
      id = 1,
      title = "Title",
      description = "Description",
      language = norwegian_bokmal,
      readingLevel = Some("1"),
      categories = Seq(
        Category(1, 1, "some-category")),
      coverPhoto = None,
      dateArrived = today,
      highlightTitle = Some("Title"),
      highlightDescription = Some("Description"))

    val BookInAmharic: api.BookHit = DefaultBookHit.copy(language = amharic)

    val DefaultFeedDefinition = api.FeedDefinition(1, 1, "some-url", "some-uuid")
    val DefaultFacets = Seq(
      api.Facet("https://opds.test.digitallibrary.io/ben/root.xml", "Bengali", "Languages", isActive = false),
      api.Facet("https://opds.test.digitallibrary.io/eng/root.xml", "English", "Languages", isActive = true),
      api.Facet("https://opds.test.digitallibrary.io/hin/root.xml", "Hindu", "Languages", isActive = false),
      api.Facet("https://opds.test.digitallibrary.io/eng/root.xml", "New arrivals", "Selection", isActive = false),
      api.Facet("https://opds.test.digitallibrary.io/eng/level1.xml", "Level 1", "Selection", isActive = false),
      api.Facet("https://opds.test.digitallibrary.io/eng/level2.xml", "Level 2", "Selection", isActive = true),
      api.Facet("https://opds.test.digitallibrary.io/eng/level3.xml", "Level 3", "Selection", isActive = false)
    )
    val DefaultFeed = api.Feed(DefaultFeedDefinition, "default title", Some("default description"), Some("default-rel"), ZonedDateTime.now().minusDays(1), Seq(), DefaultFacets)
    val DefaultFeedEntry = api.FeedEntry(DefaultBook, Seq())
    val DefaultFeedCategory = api.FeedCategory("some-url", "some-title", 1)
  }

  object Domain {
    val DefaultSource = "storyweaver"
    val DefaultPublisher = domain.Publisher(Some(1), Some(1), "Default publisher")
    val DefaultLicense = domain.License(Some(1), Some(1), "Default License", Some("Default license description"), Some("http://some.url.com"))
    val DefaultCategory = domain.Category(Some(1), Some(1), "Default Category")

    val DefaultBook = domain.Book(
      id = Some(1),
      revision = Some(1),
      publisherId = DefaultPublisher.id.get,
      licenseId = DefaultLicense.id.get,
      publisher = DefaultPublisher,
      license = DefaultLicense,
      source = DefaultSource)

    val DefaultTranslationId = 1
    val DefaultChapter = domain.Chapter(Some(1), Some(1), DefaultTranslationId, 1, Some("Default chapter title"), "Chapter-content", ChapterType.Content)

    val DefaultPerson = domain.Person(Some(1), Some(1), "Default person name", Some("abc-def"))
    val DefaultContributor = domain.Contributor(Some(1), Some(1), DefaultPerson.id.get, DefaultTranslationId, ContributorType.Author, DefaultPerson)

    val DefaultTranslation: domain.Translation = domain.Translation(
      id = Some(DefaultTranslationId),
      revision = Some(1),
      bookId = DefaultBook.id.get,
      externalId = Some("ext-id"),
      uuid = DefaultUuid,
      title = "Default translation title",
      about = "Default translation description",
      numPages = Some(10),
      language = LanguageTag(DefaultLanguage),
      translatedFrom = None,
      datePublished = Some(today),
      dateCreated = Some(yesterday),
      dateArrived = today,
      publishingStatus = domain.PublishingStatus.PUBLISHED,
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
      categories = Seq(DefaultCategory),
      bookFormat = BookFormat.HTML
    )

    val AmharicTranslation: domain.Translation = DefaultTranslation.copy(id = Some(2), language = LanguageTag(LanguageCodeAmharic))

    val DefaultinTranslation = domain.InTranslation(Some(1), Some(1), Seq("123"), 1, Some(2), LanguageTag("nob"), LanguageTag("eng"), "en", "abc")
    val DefaultInTranslationFile = domain.InTranslationFile(Some(1), Some(1), 1, domain.FileType.CONTENT, Some(1), 0, "filename", "fileId", domain.TranslationStatus.IN_PROGRESS, Some("asdfa342"))
  }

  object Crowdin {
    val DefaultContentCrowdinFile = crowdin.CrowdinFile(Some(1), 0, domain.FileType.CONTENT, crowdin.AddedFile(1, "filename", 1, 1))
    val DefaultMetadataCrowdinFile = crowdin.CrowdinFile(Some(2), 0, domain.FileType.METADATA, crowdin.AddedFile(1, "filename", 1, 1))
    val DefaultTranslatedChapter = TranslatedChapter(None, "Some content", Some("123"))
    val DefaultBookMetaData = BookMetaData("Title", "Description", Some("123"))
  }

  object Internal {
    val DefaultNewTranslatedChapter = NewTranslatedChapter(1, Some("title"), "Some content", 1)
  }

}
