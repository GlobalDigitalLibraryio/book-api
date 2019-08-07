/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi

import com.amazonaws.services.apigateway.AmazonApiGateway
import javax.sql
import com.amazonaws.services.s3.AmazonS3
import io.digitallibrary.network.GdlClient
import io.digitallibrary.bookapi.controller._
import io.digitallibrary.bookapi.integration.crowdin.CrowdinClientBuilder
import io.digitallibrary.bookapi.integration._
import io.digitallibrary.bookapi.model.api.{SearchResult, SearchResultV2}
import io.digitallibrary.bookapi.repository._
import io.digitallibrary.bookapi.service._
import io.digitallibrary.bookapi.service.search._
import io.digitallibrary.bookapi.service.translation._
import org.mockito.Mockito

trait TestEnvironment
  extends DataSource
    with ReadService
    with ReadServiceV2
    with SourceRepository
    with SourceController
    with SourceControllerV2
    with WriteService
    with WriteServiceV2
    with ElasticClient
    with ConverterService
    with ContentConverter
    with TestTransactionHandler
    with BooksController
    with BooksControllerV2
    with LanguageController
    with LevelController
    with HealthController
    with GdlClient
    with ImageApiClient
    with MediaApiClient
    with DownloadController
    with ValidationService
    with BookRepository
    with CategoryRepository
    with ChapterRepository
    with ContributorRepository
    with TranslationRepository
    with EducationalAlignmentRepository
    with PersonRepository
    with PublisherRepository
    with EPubService
    with PdfService
    with PdfServiceV2
    with FeedRepository
    with FeedService
    with FeedLocalizationService
    with OPDSController
    with TranslationsController
    with TranslationsControllerV2
    with SupportedLanguageService
    with CrowdinClientBuilder
    with FeaturedContentRepository
    with TranslationService
    with TranslationServiceV2
    with FeaturedContentController
    with FeaturedContentControllerV2
    with TranslationDbService
    with InTranslationRepository
    with InTranslationFileRepository
    with MergeService
    with IndexBuilderService
    with IndexService
    with SearchService
    with SearchServiceV2
    with SearchController
    with AmazonClient
    with CategoriesController
    with CategoriesControllerV2
    with ImportService
    with ExportController
    with ExportService
    with SynchronizeService
{

  val dataSource = mock[sql.DataSource]
  val amazonS3Client = mock[AmazonS3]
  val awsApiGatewayClient = mock[AmazonApiGateway]

  val readService = mock[ReadService]
  val readServiceV2 = mock[ReadServiceV2]
  val writeService = mock[WriteService]
  val writeServiceV2 = mock[WriteServiceV2]
  val converterService = mock[ConverterService]

  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val booksController = mock[BooksController]
  val booksControllerV2 = mock[BooksControllerV2]
  val languageController = mock[LanguageController]
  val levelController = mock[LevelController]
  val featuredContentController = mock[FeaturedContentController]
  val featuredContentControllerV2 = mock[FeaturedContentControllerV2]

  val sourceController = mock[SourceController]
  val sourceControllerV2 = mock[SourceControllerV2]
  val sourceRepository = mock[SourceRepository]

  val esClient = mock[E4sClient]

  val gdlClient = mock[GdlClient]
  val imageApiClient = mock[ImageApiClient]
  val mediaApiClient = mock[MediaApiClient]
  val downloadController = mock[DownloadController]
  val validationService = mock[ValidationService]
  val contentConverter = mock[ContentConverter]
  val bookRepository = mock[BookRepository]
  val categoryRepository = mock[CategoryRepository]
  val chapterRepository = mock[ChapterRepository]
  val contributorRepository = mock[ContributorRepository]
  val unFlaggedTranslationsRepository = mock[TranslationRepository]
  val allTranslationsRepository = mock[TranslationRepository]
  val educationalAlignmentRepository = mock[EducationalAlignmentRepository]
  val personRepository = mock[PersonRepository]
  val publisherRepository = mock[PublisherRepository]
  val ePubService = mock[EPubService]
  val pdfService = mock[PdfService]
  val pdfServiceV2 = mock[PdfServiceV2]
  val feedRepository = mock[FeedRepository]
  val feedService = mock[FeedService]
  val feedLocalizationService = mock[FeedLocalizationService]
  val opdsController = mock[OPDSController]
  val translationsController = mock[TranslationsController]
  val translationsControllerV2 = mock[TranslationsControllerV2]
  val supportedLanguageService = mock[SupportedLanguageService]
  val crowdinClientBuilder = mock[CrowdinClientBuilder]
  val featuredContentRepository = mock[FeaturedContentRepository]
  val translationService = mock[TranslationService]
  val translationServiceV2 = mock[TranslationServiceV2]
  val translationDbService = mock[TranslationDbService]
  val inTranslationRepository = mock[InTranslationRepository]
  val inTranslationFileRepository = mock[InTranslationFileRepository]
  val mergeService = mock[MergeService]
  val indexBuilderService = mock[IndexBuilderService]
  val indexService = mock[IndexService]
  val searchService = mock[SearchService]
  val searchServiceV2 = mock[SearchServiceV2]
  val searchController = mock[SearchController]
  val categoriesController = mock[CategoriesController]
  val categoriesControllerV2 = mock[CategoriesControllerV2]
  val importService = mock[ImportService]
  val exportController = mock[ExportController]
  val exportService = mock[ExportService]
  val synchronizeService = mock[SynchronizeService]

  def resetMocks() = {
    Mockito.reset(
      dataSource,
      readService,
      readServiceV2,
      writeService,
      writeServiceV2,
      converterService,
      resourcesApp,
      healthController,
      booksController,
      booksControllerV2,
      languageController,
      levelController,
      featuredContentController,
      featuredContentControllerV2,
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
      unFlaggedTranslationsRepository,
      allTranslationsRepository,
      educationalAlignmentRepository,
      personRepository,
      publisherRepository,
      ePubService,
      pdfService,
      pdfServiceV2,
      feedRepository,
      feedService,
      feedLocalizationService,
      opdsController,
      translationsController,
      translationsControllerV2,
      supportedLanguageService,
      crowdinClientBuilder,
      featuredContentRepository,
      translationService,
      translationServiceV2,
      translationDbService,
      inTranslationRepository,
      inTranslationFileRepository,
      mergeService,
      categoriesController,
      categoriesControllerV2,
      importService,
      exportController,
      exportService,
      sourceController,
      sourceControllerV2,
      sourceRepository,
      synchronizeService
    )
  }
}
