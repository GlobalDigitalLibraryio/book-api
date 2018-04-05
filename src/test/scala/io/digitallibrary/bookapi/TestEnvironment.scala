/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi

import javax.sql

import com.amazonaws.services.s3.AmazonS3
import io.digitallibrary.network.GdlClient
import io.digitallibrary.bookapi.controller._
import io.digitallibrary.bookapi.integration.crowdin.CrowdinClientBuilder
import io.digitallibrary.bookapi.integration._
import io.digitallibrary.bookapi.repository._
import io.digitallibrary.bookapi.service._
import io.digitallibrary.bookapi.service.search.{IndexBuilderService, IndexService, SearchService}
import io.digitallibrary.bookapi.service.translation.{MergeService, SupportedLanguageService, TranslationDbService, TranslationService}
import org.mockito.Mockito

trait TestEnvironment
  extends DataSource
    with ReadService
    with WriteService
    with ElasticClient
    with ConverterService
    with ContentConverter
    with TestTransactionHandler
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
    with FeedLocalizationService
    with OPDSController
    with TranslationsController
    with SupportedLanguageService
    with CrowdinClientBuilder
    with FeaturedContentRepository
    with TranslationService
    with FeaturedContentController
    with TranslationDbService
    with InTranslationRepository
    with InTranslationFileRepository
    with MergeService
    with IndexBuilderService
    with IndexService
    with SearchService
    with SearchController
    with AmazonClient
    with CategoriesController
    with ImportService
{

  val dataSource = mock[sql.DataSource]
  val amazonClient = mock[AmazonS3]

  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val converterService = mock[ConverterService]

  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val booksController = mock[BooksController]
  val languageController = mock[LanguageController]
  val levelController = mock[LevelController]
  val featuredContentController = mock[FeaturedContentController]

  val esClient = mock[E4sClient]

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
  val feedLocalizationService = mock[FeedLocalizationService]
  val opdsController = mock[OPDSController]
  val translationsController = mock[TranslationsController]
  val supportedLanguageService = mock[SupportedLanguageService]
  val crowdinClientBuilder = mock[CrowdinClientBuilder]
  val featuredContentRepository = mock[FeaturedContentRepository]
  val translationService = mock[TranslationService]
  val translationDbService = mock[TranslationDbService]
  val inTranslationRepository = mock[InTranslationRepository]
  val inTranslationFileRepository = mock[InTranslationFileRepository]
  val mergeService = mock[MergeService]
  val indexBuilderService = mock[IndexBuilderService]
  val indexService = mock[IndexService]
  val searchService = mock[SearchService]
  val searchController = mock[SearchController]
  val categoriesController = mock[CategoriesController]
  val importService = mock[ImportService]

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
      esClient,
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
      feedLocalizationService,
      opdsController,
      translationsController,
      supportedLanguageService,
      crowdinClientBuilder,
      featuredContentRepository,
      translationService,
      translationDbService,
      inTranslationRepository,
      inTranslationFileRepository,
      mergeService,
      categoriesController,
      importService
    )
  }
}
