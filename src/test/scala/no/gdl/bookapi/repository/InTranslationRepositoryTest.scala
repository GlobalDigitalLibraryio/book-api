/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain.InTranslation
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}

class InTranslationRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val inTranslationRepository = new InTranslationRepository

  val DefaultinTranslation = InTranslation(None, None, Seq("123"), 1, None, LanguageTag("nob"), LanguageTag("eng"), "abc")

  test("that inTranslation is added and retrieved") {
    withRollback { implicit session =>

      val added = inTranslationRepository.add(DefaultinTranslation)
      val retrieved = inTranslationRepository.withId(added.id.get)

      retrieved.head.id should equal(added.id)
      retrieved.head.userIds should equal(added.userIds)
      retrieved.head.fromLanguage should equal(added.fromLanguage)
      retrieved.head.toLanguage should equal(added.toLanguage)
      retrieved.head.crowdinProjectId should equal(added.crowdinProjectId)
    }
  }

  test("that forOriginalId retrieves list of books matching given id") {
    withRollback { implicit session =>
      val inTranslation1 = inTranslationRepository.add(DefaultinTranslation.copy(originalId = 1))
      val inTranslation2 = inTranslationRepository.add(DefaultinTranslation.copy(originalId = 1))
      val inTranslation3 = inTranslationRepository.add(DefaultinTranslation.copy(originalId = 1))
      val inTranslation4 = inTranslationRepository.add(DefaultinTranslation.copy(originalId = 2))
      val inTranslation5 = inTranslationRepository.add(DefaultinTranslation.copy(originalId = 2))
      val inTranslation6 = inTranslationRepository.add(DefaultinTranslation.copy(originalId = 2))

      val withOriginalId1 = inTranslationRepository.forOriginalId(1)
      withOriginalId1.length should be(3)
      withOriginalId1.forall(_.originalId == 1) should be(true)
    }
  }

  test("that update updates correct record") {
    withRollback { implicit session =>
      val inTranslation1 = inTranslationRepository.add(DefaultinTranslation)
      val inTranslation2 = inTranslationRepository.add(DefaultinTranslation)

      val updated = inTranslationRepository.updateTranslation(inTranslation1.copy(userIds = Seq("this has changed")))

      val retrieved1 = inTranslationRepository.withId(inTranslation1.id.get)
      val retrieved2 = inTranslationRepository.withId(inTranslation2.id.get)

      retrieved1.head.userIds should equal(Seq("this has changed"))
      retrieved1.head.revision.get should equal(2)
      retrieved2.head.revision.get should equal(1)
    }
  }
}
