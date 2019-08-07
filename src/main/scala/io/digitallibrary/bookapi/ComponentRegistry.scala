/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package io.digitallibrary.bookapi

import com.amazonaws.ClientConfiguration
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.{AmazonApiGateway, AmazonApiGatewayClientBuilder}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.zaxxer.hikari.HikariDataSource
import io.digitallibrary.bookapi.controller._
import io.digitallibrary.bookapi.integration._
import io.digitallibrary.bookapi.integration.crowdin.CrowdinClientBuilder
import io.digitallibrary.bookapi.model.domain.AllTranslations
import io.digitallibrary.bookapi.repository._
import io.digitallibrary.bookapi.service._
import io.digitallibrary.bookapi.service.search._
import io.digitallibrary.bookapi.service.translation._
import io.digitallibrary.network.GdlClient
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSource
  with LiveTransactionHandler
  with ReadService
  with ReadServiceV2
  with WriteService
  with WriteServiceV2
  with ElasticClient
  with ConverterService
  with ContentConverter
  with BooksController
  with LanguageController
  with LanguageControllerV2
  with LevelController
  with LevelControllerV2
  with InternController
  with HealthController
  with OPDSController
  with SourceController
  with SourceControllerV2
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
  with SourceRepository
  with EPubService
  with PdfService
  with PdfServiceV2
  with FeedRepository
  with FeedService
  with FeedLocalizationService
  with FeaturedContentRepository
  with FeaturedContentController
  with FeaturedContentControllerV2
  with TranslationsController
  with TranslationsControllerV2
  with SupportedLanguageService
  with CrowdinClientBuilder
  with TranslationService
  with TranslationDbService
  with InTranslationRepository
  with InTranslationFileRepository
  with MergeService
  with IndexService
  with IndexBuilderService
  with SearchService
  with SearchServiceV2
  with SearchController
  with SearchControllerV2
  with CategoriesController
  with CategoriesControllerV2
  with AmazonClient
  with ImportService
  with ExportController
  with ExportService
  with SynchronizeService
  with BooksControllerV2
{
  implicit val swagger = new BookSwagger

  lazy val dataSource = new HikariDataSource()
  dataSource.setJdbcUrl(BookApiProperties.DBConnectionUrl)
  dataSource.setUsername(BookApiProperties.MetaUserName)
  dataSource.setPassword(BookApiProperties.MetaPassword)
  dataSource.setMaximumPoolSize(BookApiProperties.MetaMaxConnections)
  dataSource.setSchema(BookApiProperties.MetaSchema)
  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  val amazonS3Client: AmazonS3 = {
    val commonClient = AmazonS3ClientBuilder
      .standard()
      .withClientConfiguration(
        new ClientConfiguration()
          .withTcpKeepAlive(false)
      )

    (BookApiProperties.Environment match {
      case "local" =>
        commonClient
          .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://minio.gdl-local:9000", Regions.EU_CENTRAL_1.name()))
          .withPathStyleAccessEnabled(true)
      case _ =>
        commonClient.withRegion(Regions.EU_CENTRAL_1)
    }).build()
  }

  val awsApiGatewayClient: AmazonApiGateway =
    AmazonApiGatewayClientBuilder.standard().withClientConfiguration(new ClientConfiguration().withTcpKeepAlive(false)).withRegion(Regions.EU_CENTRAL_1).build()

  lazy val booksController = new BooksController
  lazy val booksControllerV2 = new BooksControllerV2
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val internController = new InternController
  lazy val opdsController = new OPDSController
  lazy val featuredContentController = new FeaturedContentController
  lazy val featuredContentControllerV2 = new FeaturedContentControllerV2
  lazy val sourceController = new SourceController
  lazy val sourceControllerV2 = new SourceControllerV2

  lazy val readService = new ReadService
  lazy val readServiceV2 = new ReadServiceV2
  lazy val writeService = new WriteService
  lazy val writeServiceV2 = new WriteServiceV2
  lazy val converterService = new ConverterService

  lazy val esClient: E4sClient = EsClientFactory.getClient()
  lazy val gdlClient = new GdlClient
  lazy val imageApiClient = new ImageApiClient
  lazy val mediaApiClient = new MediaApiClient
  lazy val downloadController = new DownloadController
  lazy val validationService = new ValidationService
  lazy val contentConverter = new ContentConverter
  lazy val languageController = new LanguageController
  lazy val languageControllerV2 = new LanguageControllerV2
  lazy val levelController = new LevelController
  lazy val levelControllerV2 = new LevelControllerV2
  lazy val bookRepository = new BookRepository
  lazy val categoryRepository = new CategoryRepository
  lazy val chapterRepository = new ChapterRepository
  lazy val contributorRepository = new ContributorRepository
  lazy val unFlaggedTranslationsRepository = new TranslationRepository
  lazy val allTranslationsRepository = new TranslationRepository(translationView = AllTranslations)
  lazy val educationalAlignmentRepository = new EducationalAlignmentRepository
  lazy val personRepository = new PersonRepository
  lazy val publisherRepository = new PublisherRepository
  lazy val featuredContentRepository = new FeaturedContentRepository
  lazy val sourceRepository = new SourceRepository

  lazy val ePubService = new EPubService
  lazy val pdfService = new PdfService
  lazy val pdfServiceV2 = new PdfServiceV2
  lazy val feedRepository = new FeedRepository
  lazy val feedService = new FeedService
  lazy val translationsController = new TranslationsController
  lazy val translationsControllerV2 = new TranslationsControllerV2
  lazy val supportedLanguageService = new SupportedLanguageService
  lazy val crowdinClientBuilder = new CrowdinClientBuilder
  lazy val translationService = new TranslationService
  lazy val translationDbService = new TranslationDbService
  lazy val inTranslationRepository = new InTranslationRepository
  lazy val inTranslationFileRepository = new InTranslationFileRepository
  lazy val mergeService = new MergeService
  lazy val indexService = new IndexService
  lazy val indexBuilderService = new IndexBuilderService
  lazy val searchService = new SearchService
  lazy val searchServiceV2 = new SearchServiceV2
  lazy val searchController = new SearchController
  lazy val searchControllerV2 = new SearchControllerV2
  lazy val categoriesController = new CategoriesController
  lazy val categoriesControllerV2 = new CategoriesControllerV2
  lazy val importService = new ImportService
  lazy val exportController = new ExportController
  lazy val exportService = new ExportService
  lazy val synchronizeService = new SynchronizeService

  // Non-lazy because we want it to fail immediately if something goes wrong
  val feedLocalizationService = new FeedLocalizationService
}
