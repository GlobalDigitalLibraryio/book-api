/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi

import io.digitallibrary.network.GdlClient
import no.gdl.bookapi.controller._
import no.gdl.bookapi.integration.{DataSource, ElasticClient, ImageApiClient, JestClientFactory}
import no.gdl.bookapi.repository.TransactionHandler
import no.gdl.bookapi.service._
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSource
  with TransactionHandler
  with ReadService
  with WriteService
  with ElasticClient
  with ConverterService
  with ContentConverter
  with BooksController
  with LanguageController
  with LevelController
  with EditorPickController
  with InternController
  with HealthController
  with OPDSController
  with GdlClient
  with ImageApiClient
  with DownloadController
  with ValidationService
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

  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val converterService = new ConverterService

  lazy val jestClient = JestClientFactory.getClient()
  lazy val gdlClient = new GdlClient
  lazy val imageApiClient = new ImageApiClient
  lazy val downloadController = new DownloadController
  lazy val validationService = new ValidationService
  lazy val contentConverter = new ContentConverter
  lazy val languageController = new LanguageController
  lazy val levelController = new LevelController
  lazy val editorPickController = new EditorPickController
}
