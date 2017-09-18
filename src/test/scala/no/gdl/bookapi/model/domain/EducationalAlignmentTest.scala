/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}

class EducationalAlignmentTest extends IntegrationSuite with TestEnvironment {

  test("that EducationalAlignment is added") {
    withRollback { implicit session =>
      val educationalAlignmentDef = EducationalAlignment(None, None, Some("alignmentType"), None, None, None, None)

      val inserted = EducationalAlignment.add(educationalAlignmentDef)

      val withId = EducationalAlignment.withId(inserted.id.get)
      withId.isDefined should be(true)
      withId.head.id.isDefined should be(true)
      withId.head.revision.isDefined should be(true)
      withId.head.alignmentType should equal(Some("alignmentType"))
    }
  }

  test("that withId returns None when id does not exist") {
    val withId = EducationalAlignment.withId(100)
    withId should be (None)
  }
}
