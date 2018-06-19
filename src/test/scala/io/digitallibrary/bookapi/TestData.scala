/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import io.digitallibrary.bookapi.integration.crowdin.{BookMetaData, TranslatedChapter}
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.api.internal.NewTranslatedChapter
import io.digitallibrary.bookapi.model.domain.{BookFormat, ChapterType, ContributorType, PageOrientation, PublishingStatus}
import io.digitallibrary.language.model.LanguageTag

object TestData {
  val LanguageCodeNorwegian = "nb"
  val LanguageCodeEnglish = "en"
  val LanguageCodeAmharic = "am"

  val DefaultUuid = UUID.randomUUID().toString
  val DefaultLanguage = LanguageCodeNorwegian
  val Level1 = "1"
  val ageRangeDefault = "5-10"
  val today = LocalDate.now()
  val yesterday = today.minusDays(1)
  val epub = "url-to-epub"
  val pdf = "url-to-pdf"

  val validTestToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9pZCI6IjEyMyIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UifQ.e3BKK_gLxWQwJhFX6SppNchM_eSwu82yKghVx2P3yMY"
  val validTestTokenWithWriteRole = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9pZCI6IjEyMyIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJzY29wZSI6ImJvb2tzLWxvY2FsOndyaXRlIn0.RNLeTpQogFoHRgwz5bJN2INvszK-YSgiJS4yatJvvFs"
  val validTestTokenWithFeaturedRole = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9pZCI6IjEyMyIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJzY29wZSI6ImJvb2tzLWxvY2FsOmZlYXR1cmVkIn0.lvUkAaez_uJzxFG4GJeXxKOdmMdqN3oNJttMYsozkzs"
  val invalidTestToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9hYmMiOiIxMjMiLCJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.5rtcIdtPmH3AF1pwNbNvBMKmulyiEoWZfn1ip9aMzv4"

  object Api {
    val english = api.Language(LanguageCodeEnglish, "English")
    val norwegian_bokmal = api.Language(LanguageCodeNorwegian, "Bokmål, Norwegian")
    val amharic = api.Language(LanguageCodeAmharic, "Amharic")

    val DefaultLicense = api.License(1, 1, "lisens", Some("beskrivelse"), Some("url"))
    val DefaultPublisher = api.Publisher(1, 1, "Some Publisher")
    val DefaultSource = "storyweaver"
    val DefaultPageOrientation = "PORTRAIT"

    val category1 = api.Category(1, 1, "category1")
    val category2 = api.Category(2, 1, "category2")

    val author1 = api.Contributor(1, 1, ContributorType.Author.toString, "Author Authorson")
    val author2 = api.Contributor(2, 1, ContributorType.Author.toString, "Co Author")
    val photographer = api.Contributor(3, 1, ContributorType.Photographer.toString, "Photo Grapher")
    val illustrator = api.Contributor(4, 1, ContributorType.Illustrator.toString, "Illu Strator")

    val ChapterSummary1 = api.ChapterSummary(1, 1, Some("Title"), "some-url")
    val Chapter1 = api.Chapter(1, 1, 1, Some("Title"), "Content", "Content")

    val DefaultBook = api.Book(
      id = 1,
      revision = 1,
      externalId = Some("external-1"),
      uuid = DefaultUuid,
      title = "Title",
      description = "Description",
      translatedFrom = None, language = english,
      availableLanguages = Seq(english, norwegian_bokmal),
      license = DefaultLicense,
      publisher = DefaultPublisher,
      readingLevel = Some(Level1),
      typicalAgeRange = Some(ageRangeDefault),
      educationalUse = None,
      educationalRole = None,
      timeRequired = None,
      datePublished = Some(today),
      dateCreated = Some(yesterday),
      dateArrived = today,
      categories = Seq(category1, category2),
      coverImage = None,
      downloads = api.Downloads(Some(epub), None),
      tags = Seq(),
      contributors = Seq(author1, author2, photographer, illustrator),
      chapters = Seq(ChapterSummary1),
      supportsTranslation = true,
      bookFormat = BookFormat.HTML.toString,
      source = DefaultSource,
      pageOrientation = DefaultPageOrientation,
      publishingStatus = PublishingStatus.PUBLISHED.toString)

    val DefaultBookHit = api.BookHit(
      id = 1,
      title = "Title",
      description = "Description",
      language = norwegian_bokmal,
      readingLevel = Some("1"),
      categories = Seq(
        Category
        (1, 1, "library_books")),
      coverImage = None,
      dateArrived = today,
      source = DefaultSource,
      highlightTitle = Some("Title"),
      highlightDescription = Some("Description"))

    val BookInAmharic: api.BookHit = DefaultBookHit.copy(language = amharic)

    val DefaultFeedDefinition = api.FeedDefinition(1, 1, "some-url", "some-uuid")
    val DefaultFacets = Seq(
      api.Facet("https://opds.test.digitallibrary.io/v1/ben/root.xml", "Bengali", "Languages", isActive = false),
      api.Facet("https://opds.test.digitallibrary.io/v1/eng/root.xml", "English", "Languages", isActive = true),
      api.Facet("https://opds.test.digitallibrary.io/v1/hin/root.xml", "Hindu", "Languages", isActive = false),
      api.Facet("https://opds.test.digitallibrary.io/v1/eng/root.xml", "New arrivals", "Selection", isActive = false),
      api.Facet("https://opds.test.digitallibrary.io/v1/eng/level1.xml", "Level 1", "Selection", isActive = false),
      api.Facet("https://opds.test.digitallibrary.io/v1/eng/level2.xml", "Level 2", "Selection", isActive = true),
      api.Facet("https://opds.test.digitallibrary.io/v1/eng/level3.xml", "Level 3", "Selection", isActive = false)
    )
    val DefaultFeed = api.Feed(DefaultFeedDefinition, "default title", Some("default description"), Some("default-rel"), ZonedDateTime.now().minusDays(1), Seq(), DefaultFacets)
    val DefaultFeedEntry = api.FeedEntry(DefaultBook, Seq())
    val DefaultFeedCategory = api.FeedCategory("some-url", "some-title", 1)
  }

  object Domain {
    val DefaultSource = "storyweaver"
    val DefaultPageOrientation = PageOrientation.PORTRAIT
    val DefaultPublisher = domain.Publisher(Some(1), Some(1), "Default publisher")
    val DefaultLicense = domain.License(Some(1), Some(1), "Default License", Some("Default license description"), Some("http://some.url.com"))
    val DefaultCategory = domain.Category(Some(1), Some(1), "library_books")

    val DefaultBook = domain.Book(
      id = Some(1),
      revision = Some(1),
      publisherId = DefaultPublisher.id.get,
      licenseId = DefaultLicense.id.get,
      publisher = DefaultPublisher,
      license = DefaultLicense,
      source = DefaultSource)

    val DefaultTranslationId = 1
    val DefaultChapter = domain.Chapter(id = Some(1), revision = Some(1), translationId = DefaultTranslationId, seqNo = 1, title = Some("Default chapter title"), content = "Chapter-content", chapterType = ChapterType.Content)

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
      bookFormat = BookFormat.HTML,
      pageOrientation = PageOrientation.PORTRAIT
    )

    val AmharicTranslation: domain.Translation = DefaultTranslation.copy(id = Some(2), language = LanguageTag(LanguageCodeAmharic))

    val DefaultinTranslation = domain.InTranslation(Some(1), Some(1), Seq("123"), 1, Some(2), LanguageTag("nb"), LanguageTag("en"), "en", "abc")
    val DefaultInTranslationFile = domain.InTranslationFile(Some(1), Some(1), 1, domain.FileType.CONTENT, Some(1), 0, "filename", "fileId", domain.TranslationStatus.IN_PROGRESS, Some("asdfa342"))
  }

  object Crowdin {
    val DefaultContentCrowdinFile = crowdin.CrowdinFile(Some(1), 0, domain.FileType.CONTENT, crowdin.AddedFile(1, "filename", 1, 1))
    val DefaultMetadataCrowdinFile = crowdin.CrowdinFile(Some(2), 0, domain.FileType.METADATA, crowdin.AddedFile(1, "filename", 1, 1))
    val DefaultTranslatedChapter = TranslatedChapter(None, "Some content", Some("123"))
    val DefaultBookMetaData = BookMetaData("Title", "Description", Some("Translator 1, Translator"), Some("123"))
  }

  object Internal {
    val DefaultNewTranslatedChapter = NewTranslatedChapter(1, Some("title"), "Some content", 1)
    val DefaultInternalBook = api.internal.Book(
      id = 1,
      revision = 1,
      externalId = Some("1"),
      uuid = "some-uuid",
      title = "Some title",
      description = "Some description",
      translatedFrom = None,
      language = api.Language("nb", "Norsk Bokmål"),
      availableLanguages = Seq(),
      license = Api.DefaultLicense,
      publisher = Api.DefaultPublisher,
      readingLevel = Some("1"),
      typicalAgeRange = None,
      educationalUse = None,
      educationalRole = None,
      timeRequired = None,
      datePublished = None,
      dateCreated = None,
      dateArrived = today,
      categories = Seq(),
      coverPhoto = None,
      downloads = api.Downloads(None, None),
      tags = Seq(),
      contributors = Seq(Api.author1),
      chapters = Seq(),
      supportsTranslation = false,
      bookFormat = BookFormat.HTML.toString,
      source = "storyweaver",
      pageOrientation = PageOrientation.PORTRAIT.toString
    )
  }

}
