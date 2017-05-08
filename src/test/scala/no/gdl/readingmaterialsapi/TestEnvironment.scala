/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi

import javax.sql

import no.gdl.readingmaterialsapi.controller.{HealthController, ReadingMaterialsController}
import no.gdl.readingmaterialsapi.integration.{DataSource, ElasticClient, NdlaJestClient}
import no.gdl.readingmaterialsapi.repository.ReadingMaterialsRepository
import no.gdl.readingmaterialsapi.service.{ConverterService, ReadService, WriteService}
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
  extends DataSource
  with ReadService
  with WriteService
  with ElasticClient
  with ConverterService
  with ReadingMaterialsRepository
  with ReadingMaterialsController
  with HealthController
{
  val dataSource = mock[sql.DataSource]
  val readingMaterialsRepository = mock[ReadingMaterialsRepository]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val converterService = mock[ConverterService]

  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val readingMaterialsController = mock[ReadingMaterialsController]

  val jestClient = mock[NdlaJestClient]
}
