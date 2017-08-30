/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi

import no.gdl.bookapi.controller.{HealthController, InternController, OPDSController, BooksController}
import no.gdl.bookapi.integration.{DataSource, ElasticClient, JestClientFactory}
import no.gdl.bookapi.repository.BooksRepository
import no.gdl.bookapi.service.{ConverterService, ReadService, WriteService}
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSource
  with BooksRepository
  with ReadService
  with WriteService
  with ElasticClient
  with ConverterService
  with BooksController
  with InternController
  with HealthController
  with OPDSController
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

  lazy val booksRepository = new BooksRepository
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val converterService = new ConverterService

  lazy val jestClient = JestClientFactory.getClient()
}
