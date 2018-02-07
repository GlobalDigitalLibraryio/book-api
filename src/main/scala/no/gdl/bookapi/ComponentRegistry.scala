/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi

import io.digitallibrary.network.GdlClient
import no.gdl.bookapi.controller._
import no.gdl.bookapi.integration.crowdin.CrowdinClientBuilder
import no.gdl.bookapi.integration._
import no.gdl.bookapi.repository._
import no.gdl.bookapi.service._
import no.gdl.bookapi.service.translation.{MergeService, SupportedLanguageService, TranslationDbService, TranslationService}
import org.postgresql.ds.PGPoolingDataSource
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
{
  implicit val swagger = new BookSwagger

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(BookApiProperties.MetaUserName)
  dataSource.setPassword(BookApiProperties.MetaPassword)
  dataSource.setDatabaseName(BookApiProperties.MetaResource)
  dataSource.setServerName(BookApiProperties.MetaServer)
  dataSource.setPortNumber(BookApiProperties.MetaPort)
  dataSource.setInitialConnections(BookApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(BookApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(BookApiProperties.MetaSchema)
  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  lazy val booksController = new BooksController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val internController = new InternController
  lazy val opdsController = new OPDSController
  lazy val featuredContentController = new FeaturedContentController

  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val converterService = new ConverterService

  lazy val jestClient: NdlaJestClient = JestClientFactory.getClient()
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
}
