/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.TestData.Domain.{DefaultInTranslationFile, DefaultinTranslation}
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}

class InTranslationFileRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val inTranslationFileRepository = new InTranslationFileRepository
  override val inTranslationRepository = new InTranslationRepository

  test("that inTranslationFile is added and retrieved") {
    withRollback { implicit session =>
      val inTranslation = inTranslationRepository.add(DefaultinTranslation)

      val added = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation.id.get))
      val retrieved = inTranslationFileRepository.withId(added.id.get)

      retrieved should equal(Some(added))
    }
  }

  test("that withTranslationId retrieves all files belonging to the given translationId") {
    withRollback { implicit session =>
      val inTranslation1 = inTranslationRepository.add(DefaultinTranslation.copy(originalTranslationId = 1, crowdinProjectId = "abc", toLanguage = LanguageTag("nob")))
      val inTranslation2 = inTranslationRepository.add(DefaultinTranslation.copy(originalTranslationId = 2, crowdinProjectId = "abc", toLanguage = LanguageTag("nob")))

      val addedFile1 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation1.id.get))
      val addedFile2 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation1.id.get))
      val addedFile3 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation2.id.get))
      val addedFile4 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(inTranslationId = inTranslation2.id.get))

      val files = inTranslationFileRepository.withTranslationId(inTranslation1.id.get)
      files.length should be(2)
      files.forall(_.inTranslationId == inTranslation1.id.get) should be(true)
    }
  }

  test("that forCrowdinProjectWithFileIdAndLanguage retrieves the correct file id") {
    withRollback { implicit session =>
      val inTranslation1 = inTranslationRepository.add(DefaultinTranslation.copy(originalTranslationId = 1, crowdinProjectId = "this", toLanguage = LanguageTag("nob")))
      val inTranslation2 = inTranslationRepository.add(DefaultinTranslation.copy(originalTranslationId = 1, crowdinProjectId = "notthis", toLanguage = LanguageTag("nob")))

      val addedFile1 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(crowdinFileId = "1", inTranslationId = inTranslation1.id.get))
      val addedFile2 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(crowdinFileId = "2", inTranslationId = inTranslation1.id.get))
      val addedFile3 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(crowdinFileId = "1", inTranslationId = inTranslation2.id.get))
      val addedFile4 = inTranslationFileRepository.add(DefaultInTranslationFile.copy(crowdinFileId = "2", inTranslationId = inTranslation2.id.get))

      val retrieved = inTranslationFileRepository.forCrowdinProjectWithFileIdAndLanguage("this", "1", LanguageTag("nob"))
      retrieved.isDefined should be (true)
      retrieved.get.id should equal (addedFile1.id)
    }
  }
}
