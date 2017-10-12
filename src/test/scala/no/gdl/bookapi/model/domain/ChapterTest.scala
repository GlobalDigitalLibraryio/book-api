/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.{TestData, UnitSuite}


class ChapterTest extends UnitSuite {
  test("that extracting imageIds from chapter without images returns empty Seq()") {
    val contentWithoutImages =
      """
        |<div>
        |<p>Some text here </p>
        |<p>
        |   Some more text
        |   that is very interesting.
        |</p>
        |</div>
      """.stripMargin

    val chapter = TestData.Domain.DefaultChapter.copy(content = contentWithoutImages)
    chapter.imagesInChapter() should equal(Seq())
  }

  test("that extracting imageIds from chapter content returns all image ids") {
    val contentWithImages =
      """
        |<div>
        |<p>Some text here </p>
        |<p>
        |   Text before an image
        |   <embed data-resource="image" data-resource_id="1"/>
        |   Text after an image
        |</p>
        |<embed data-resource="image" data-resource_id="2"/>
        |<embed data-resource="image" data-resource_id="3"/>
        |</div>
      """.stripMargin

    val chapter = TestData.Domain.DefaultChapter.copy(content = contentWithImages)
    chapter.imagesInChapter() should equal(Seq(1, 2, 3))
  }
}
