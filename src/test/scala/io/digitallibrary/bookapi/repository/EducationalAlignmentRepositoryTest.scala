/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.bookapi.model.domain.EducationalAlignment
import io.digitallibrary.bookapi.{IntegrationSuite, TestEnvironment}

class EducationalAlignmentRepositoryTest extends IntegrationSuite with TestEnvironment {

  override val educationalAlignmentRepository = new EducationalAlignmentRepository

  test("that EducationalAlignment is added") {
    withRollback { implicit session =>
      val educationalAlignmentDef = EducationalAlignment(None, None, Some("alignmentType"), None, None, None, None)

      val inserted = educationalAlignmentRepository.add(educationalAlignmentDef)

      val withId = educationalAlignmentRepository.withId(inserted.id.get)
      withId.isDefined should be(true)
      withId.head.id.isDefined should be(true)
      withId.head.revision.isDefined should be(true)
      withId.head.alignmentType should equal(Some("alignmentType"))
    }
  }

  test("that withId returns None when id does not exist") {
    val withId = educationalAlignmentRepository.withId(100)
    withId should be (None)
  }
}
