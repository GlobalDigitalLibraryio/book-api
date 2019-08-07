package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.{BookSwagger, TestEnvironment, UnitSuite}
import io.digitallibrary.bookapi.model.domain.Category
import io.digitallibrary.language.model.LanguageTag
import org.mockito.Mockito.{verify, when}
import org.scalatra.test.scalatest.ScalatraFunSuite

class CategoriesControllerV2Test extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controllerV2 = new CategoriesControllerV2
  implicit val swagger: BookSwagger = new BookSwagger

  val categories: Map[Category, Set[String]] = Map(
    Category(None, None, "cat1") -> Set("level1", "level2"),
    Category(None, None, "cat2") -> Set("level2", "level3")
  )

  override def beforeEach: Unit = {
    resetMocks()
  }

  addServlet(controllerV2, "/*")

  test("v2: that / defaults to english language") {
    get("/") {}
    verify(readServiceV2).listAvailablePublishedCategoriesForLanguage(LanguageTag("eng"))
  }

  test("v2: that /eng returns 200 ok with empty result set for language with no books") {
    when(readServiceV2.listAvailablePublishedCategoriesForLanguage(LanguageTag("eng"))).thenReturn(Map.empty[Category, Set[String]])
    get("/eng") {
      status should equal(200)
      body should equal("{}")
    }
    verify(readServiceV2).listAvailablePublishedCategoriesForLanguage(LanguageTag("eng"))
  }

  test("v2: that /eng returns 200 ok with expected result set for language with books") {
    when(readServiceV2.listAvailablePublishedCategoriesForLanguage(LanguageTag("eng"))).thenReturn(categories)
    get("/eng") {
      status should equal(200)
      body should equal("{\"cat1\":{\"readingLevels\":[\"level1\",\"level2\"]},\"cat2\":{\"readingLevels\":[\"level2\",\"level3\"]}}")
    }
    verify(readServiceV2).listAvailablePublishedCategoriesForLanguage(LanguageTag("eng"))
  }

}
