/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import javax.sql

import no.gdl.bookapi.controller.{HealthController, BooksController}
import no.gdl.bookapi.integration.{DataSource, ElasticClient, NdlaJestClient}
import no.gdl.bookapi.repository.BooksRepository
import no.gdl.bookapi.service.{ConverterService, ReadService, WriteService}
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
  extends DataSource
  with ReadService
  with WriteService
  with ElasticClient
  with ConverterService
  with BooksRepository
  with BooksController
  with HealthController
{
  val dataSource = mock[sql.DataSource]
  val booksRepository = mock[BooksRepository]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val converterService = mock[ConverterService]

  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val booksController = mock[BooksController]

  val jestClient = mock[NdlaJestClient]
}
