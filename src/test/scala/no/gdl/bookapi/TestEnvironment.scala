/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import javax.sql

import io.digitallibrary.network.GdlClient
import no.gdl.bookapi.controller._
import no.gdl.bookapi.integration.crowdin.CrowdinClientBuilder
import no.gdl.bookapi.integration.{DataSource, ElasticClient, ImageApiClient, NdlaJestClient}
import no.gdl.bookapi.repository._
import no.gdl.bookapi.service._
import no.gdl.bookapi.service.translation.{SupportedLanguageService, TranslationService, WriteTranslationService}
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
  extends DataSource
    with ReadService
    with WriteService
    with ElasticClient
    with ConverterService
    with ContentConverter
    with TransactionHandler
    with BooksController
    with LanguageController
    with LevelController
    with HealthController
    with GdlClient
    with ImageApiClient
    with DownloadController
    with ValidationService
    with BookRepository
    with CategoryRepository
    with ChapterRepository
    with ContributorRepository
    with TranslationRepository
    with EducationalAlignmentRepository
    with LicenseRepository
    with PersonRepository
    with PublisherRepository
    with EPubService
    with PdfService
    with FeedRepository
    with FeedService
    with OPDSController
    with TranslationsController
    with SupportedLanguageService
    with CrowdinClientBuilder
    with FeaturedContentRepository
    with TranslationService
    with FeaturedContentController
    with WriteTranslationService
    with InTranslationRepository
    with InTranslationFileRepository {
  val dataSource = mock[sql.DataSource]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val converterService = mock[ConverterService]

  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val booksController = mock[BooksController]
  val languageController = mock[LanguageController]
  val levelController = mock[LevelController]
  val featuredContentController = mock[FeaturedContentController]

  val jestClient = mock[NdlaJestClient]

  val gdlClient = mock[GdlClient]
  val imageApiClient = mock[ImageApiClient]
  val downloadController = mock[DownloadController]
  val validationService = mock[ValidationService]
  val contentConverter = mock[ContentConverter]
  val bookRepository = mock[BookRepository]
  val categoryRepository = mock[CategoryRepository]
  val chapterRepository = mock[ChapterRepository]
  val contributorRepository = mock[ContributorRepository]
  val translationRepository = mock[TranslationRepository]
  val educationalAlignmentRepository = mock[EducationalAlignmentRepository]
  val licenseRepository = mock[LicenseRepository]
  val personRepository = mock[PersonRepository]
  val publisherRepository = mock[PublisherRepository]
  val ePubService = mock[EPubService]
  val pdfService = mock[PdfService]
  val feedRepository = mock[FeedRepository]
  val feedService = mock[FeedService]
  val opdsController = mock[OPDSController]
  val translationsController = mock[TranslationsController]
  val supportedLanguageService = mock[SupportedLanguageService]
  val crowdinClientBuilder = mock[CrowdinClientBuilder]
  val featuredContentRepository = mock[FeaturedContentRepository]
  val translationService = mock[TranslationService]
  val writeTranslationService = mock[WriteTranslationService]
  val inTranslationRepository = mock[InTranslationRepository]
  val inTranslationFileRepository = mock[InTranslationFileRepository]

  def resetMocks() = {
    Mockito.reset(
      dataSource,
      readService,
      writeService,
      converterService,
      resourcesApp,
      healthController,
      booksController,
      languageController,
      levelController,
      featuredContentController,
      jestClient,
      gdlClient,
      imageApiClient,
      downloadController,
      validationService,
      contentConverter,
      bookRepository,
      categoryRepository,
      chapterRepository,
      contributorRepository,
      translationRepository,
      educationalAlignmentRepository,
      licenseRepository,
      personRepository,
      publisherRepository,
      ePubService,
      pdfService,
      feedRepository,
      feedService,
      opdsController,
      translationsController,
      supportedLanguageService,
      crowdinClientBuilder,
      featuredContentRepository,
      translationService,
      writeTranslationService,
      inTranslationRepository,
      inTranslationFileRepository
    )
  }
}
