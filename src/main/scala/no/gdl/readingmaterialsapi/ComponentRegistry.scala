/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.readingmaterialsapi

import no.gdl.readingmaterialsapi.controller.{HealthController, InternController, OPDSController, ReadingMaterialsController}
import no.gdl.readingmaterialsapi.integration.{DataSource, ElasticClient, JestClientFactory}
import no.gdl.readingmaterialsapi.repository.ReadingMaterialsRepository
import no.gdl.readingmaterialsapi.service.{ConverterService, ReadService, WriteService}
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
  extends DataSource
  with ReadingMaterialsRepository
  with ReadService
  with WriteService
  with ElasticClient
  with ConverterService
  with ReadingMaterialsController
  with InternController
  with HealthController
  with OPDSController
{
  implicit val swagger = new ReadingMaterialsSwagger

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ReadingMaterialsApiProperties.MetaUserName)
  dataSource.setPassword(ReadingMaterialsApiProperties.MetaPassword)
  dataSource.setDatabaseName(ReadingMaterialsApiProperties.MetaResource)
  dataSource.setServerName(ReadingMaterialsApiProperties.MetaServer)
  dataSource.setPortNumber(ReadingMaterialsApiProperties.MetaPort)
  dataSource.setInitialConnections(ReadingMaterialsApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(ReadingMaterialsApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(ReadingMaterialsApiProperties.MetaSchema)
  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  lazy val readingMaterialsController = new ReadingMaterialsController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val internController = new InternController
  lazy val opdsController = new OPDSController

  lazy val readingMaterialsRepository = new ReadingMaterialsRepository
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val converterService = new ConverterService

  lazy val jestClient = JestClientFactory.getClient()
}
