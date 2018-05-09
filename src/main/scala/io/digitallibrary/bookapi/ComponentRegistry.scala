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
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.zaxxer.hikari.HikariDataSource
import io.digitallibrary.bookapi.controller._
import io.digitallibrary.bookapi.integration._
import io.digitallibrary.bookapi.integration.crowdin.CrowdinClientBuilder
import io.digitallibrary.bookapi.repository._
import io.digitallibrary.bookapi.service._
import io.digitallibrary.bookapi.service.search.{IndexBuilderService, IndexService, SearchService}
import io.digitallibrary.bookapi.service.translation.{MergeService, SupportedLanguageService, TranslationDbService, TranslationService}
import io.digitallibrary.network.GdlClient
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSource
  with LiveTransactionHandler
  with ReadService
  with WriteService
  with ElasticClient
  with ConverterService
  with ContentConverter
  with BooksController
  with LanguageController
  with LevelController
  with InternController
  with HealthController
  with OPDSController
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
  with FeaturedContentRepository
  with FeaturedContentController
  with TranslationsController
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
  with SearchController
  with CategoriesController
  with AmazonClient
  with ImportService
  with ExportController
  with ExportService
{
  implicit val swagger = new BookSwagger

  lazy val dataSource = new HikariDataSource()
  dataSource.setJdbcUrl(BookApiProperties.DBConnectionUrl)
  dataSource.setUsername(BookApiProperties.MetaUserName)
  dataSource.setPassword(BookApiProperties.MetaPassword)
  dataSource.setMaximumPoolSize(BookApiProperties.MetaMaxConnections)
  dataSource.setSchema(BookApiProperties.MetaSchema)
  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  val amazonClient: AmazonS3 = {
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

  lazy val booksController = new BooksController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val internController = new InternController
  lazy val opdsController = new OPDSController
  lazy val featuredContentController = new FeaturedContentController

  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val converterService = new ConverterService

  lazy val esClient: E4sClient = EsClientFactory.getClient()
  lazy val gdlClient = new GdlClient
  lazy val imageApiClient = new ImageApiClient
  lazy val downloadController = new DownloadController
  lazy val validationService = new ValidationService
  lazy val contentConverter = new ContentConverter
  lazy val languageController = new LanguageController
  lazy val levelController = new LevelController
  lazy val bookRepository = new BookRepository
  lazy val categoryRepository = new CategoryRepository
  lazy val chapterRepository = new ChapterRepository
  lazy val contributorRepository = new ContributorRepository
  lazy val translationRepository = new TranslationRepository
  lazy val educationalAlignmentRepository = new EducationalAlignmentRepository
  lazy val licenseRepository = new LicenseRepository
  lazy val personRepository = new PersonRepository
  lazy val publisherRepository = new PublisherRepository
  lazy val featuredContentRepository = new FeaturedContentRepository

  lazy val ePubService = new EPubService
  lazy val pdfService = new PdfService
  lazy val feedRepository = new FeedRepository
  lazy val feedService = new FeedService
  lazy val translationsController = new TranslationsController
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
  lazy val searchController = new SearchController
  lazy val categoriesController = new CategoriesController
  lazy val importService = new ImportService
  lazy val exportController = new ExportController
  lazy val exportService = new ExportService

  // Non-lazy because we want it to fail immediately if something goes wrong
  val feedLocalizationService = new FeedLocalizationService
}
