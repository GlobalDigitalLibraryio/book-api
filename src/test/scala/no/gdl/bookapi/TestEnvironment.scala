/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import javax.sql

import io.digitallibrary.network.GdlClient
import no.gdl.bookapi.controller.{BooksController, DownloadController, HealthController}
import no.gdl.bookapi.integration.{DataSource, ElasticClient, ImageApiClient, NdlaJestClient}
import no.gdl.bookapi.repository.BooksRepository
import no.gdl.bookapi.service.{ConverterService, ReadService, ValidationService, WriteService}
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
  with GdlClient
  with ImageApiClient
  with DownloadController
  with ValidationService
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

  val gdlClient = mock[GdlClient]
  val imageApiClient = mock[ImageApiClient]
  val downloadController = mock[DownloadController]
  val validationService = mock[ValidationService]
}
