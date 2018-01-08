/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain.{InTranslation, InTranslationFile}
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}

class InTranslationFileRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val inTranslationFileRepository = new InTranslationFileRepository
  override val inTranslationRepository = new InTranslationRepository

  val DefaultinTranslation = InTranslation(None, None, Seq("123"), 1, None, LanguageTag("nob"), LanguageTag("eng"), "abc")
  val DefaultInTranslationFile = InTranslationFile(None, None, 1, "content", None, "filename", "fileId", "in_progress", None)

  test("that inTranslationFile is added and retrieved") {
    withRollback { implicit session =>
      val inTranslation = inTranslationRepository.add(DefaultinTranslation)

      val added = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation.id.get))
      val retrieved = inTranslationFileRepository.withId(added.id.get)

      retrieved.head.id should equal (added.id)
    }
  }

  test("that withTranslationId retrieves all files belonging to the given translationId") {
    val inTranslation1 = inTranslationRepository.add(DefaultinTranslation)
    val inTranslation2 = inTranslationRepository.add(DefaultinTranslation)

    val addedFile1 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation1.id.get))
    val addedFile2 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation1.id.get))
    val addedFile3 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation2.id.get))
    val addedFile4 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation2.id.get))

    val files = inTranslationFileRepository.withTranslationId(inTranslation1.id)
    files.length should be (2)
    files.forall(_.inTranslationId == inTranslation1.id.get) should be (true)
  }
}
