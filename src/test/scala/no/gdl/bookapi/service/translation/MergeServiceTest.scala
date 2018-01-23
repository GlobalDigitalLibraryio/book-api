/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.jsoup.Jsoup

class MergeServiceTest extends UnitSuite with TestEnvironment {

  private val service = new MergeService
  private val originalHtml =
    """<embed data-resource="image" data-resource_id="556"/>
      |<embed data-resource="image" data-resource_id="557"/>
      |<p>
      | There was a large boulder on the grassy plain. He stumbled and fell down.
      |</p>
      |""".stripMargin

  private val translatedHtml =
    """<img src="https://images.test.digitallibrary.io/TYnPhXd4.jpg" />
      |<img src="https://images.test.digitallibrary.io/abct.jpg" />
      |<p>
      | Hjorten traff en stor stein på den grønne sletten. Han snublet og falt.
      |</p>
      |""".stripMargin

  test("that img-tags are replaced with embed-tags") {
    val original = TestData.Domain.DefaultChapter.copy(content = originalHtml)
    val translated = TestData.Crowdin.DefaultTranslatedChapter.copy(content = translatedHtml)

    val newChapter = service.mergeChapter(original, translated)
    val newContentDoc = Jsoup.parseBodyFragment(newChapter.content)
    newContentDoc.select("embed[data-resource]").size() should be (2)
    newContentDoc.getElementsByTag("img").size() should be (0)
  }

  test("that unwanted html-tags are stripped, but content kept") {
    val original = TestData.Domain.DefaultChapter.copy(content = "<p>This is content</p>")
    val translated = TestData.Crowdin.DefaultTranslatedChapter.copy(content = """<p>This is <a href="#">a link to </a>content</p>""")

    val newChapter = service.mergeChapter(original, translated)
    val contentWithoutWhitespace = newChapter.content.replaceAll("\\n", "")
    contentWithoutWhitespace should equal ("<p>This is a link to content</p>")
  }

  test("that extra inserted images are removed") {
    val original = TestData.Domain.DefaultChapter.copy(content = originalHtml)
    val translated = TestData.Crowdin.DefaultTranslatedChapter.copy(content = s"""$translatedHtml<img src="#" /><img src="#" /><img src="#" /><img src="#" />""")

    val newChapter = service.mergeChapter(original, translated)
    val newContentDoc = Jsoup.parseBodyFragment(newChapter.content)
    newContentDoc.select("embed[data-resource]").size() should be (2)
    newContentDoc.getElementsByTag("img").size() should be (0)
  }

}
