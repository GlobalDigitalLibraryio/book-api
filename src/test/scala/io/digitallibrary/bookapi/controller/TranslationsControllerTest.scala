/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.model.api.Language
import io.digitallibrary.bookapi.model.domain.{FileType, InTranslationFile, TranslationStatus}
import io.digitallibrary.bookapi.{BookSwagger, TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.Mockito.{times, verify, when}
import org.mockito.ArgumentMatchers._
import org.scalatra.swagger.Swagger
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success

class TranslationsControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite{
  implicit val swagger: Swagger = new BookSwagger
  lazy val controller = new TranslationsController

  addServlet(controller, "/*")

  test("that controller returns 200 ok") {
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(Language("amh", "Amharic")))

    get("/supported-languages") {
      status should be (200)
      body should be ("""[{"code":"amh","name":"Amharic"}]""")
    }

    verify(supportedLanguageService, times(1)).getSupportedLanguages()
  }

  test("that /file-proofread?project=test&language=nb&file_id=123 updates translation when all files are proofread") {
    when(synchronizeService.fetchTranslatedFile(any[String], any[String], any[String], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultInTranslationFile))
    when(synchronizeService.allFilesHaveTranslationStatusGreatherOrEqualTo(any[InTranslationFile], any[TranslationStatus.Value])).thenReturn(true)

    get("/file-proofread?project=test&language=nb&file_id=123") {
      status should be (204)
    }

    verify(synchronizeService).markTranslationAs(any[InTranslationFile], any[TranslationStatus.Value])
  }

}
