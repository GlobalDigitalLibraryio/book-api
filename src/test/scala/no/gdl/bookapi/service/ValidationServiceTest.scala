package no.gdl.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.api.{FeaturedContent, Language}
import no.gdl.bookapi.model.domain
import no.gdl.bookapi.model.domain.ContributorType
import no.gdl.bookapi.{TestEnvironment, UnitSuite}

import scala.util.Success

class ValidationServiceTest extends UnitSuite with TestEnvironment {

  override val validationService = new ValidationService

  private val validUrl = "http://commons.apache.org/proper/commons-validator/"
  private val validUpdatedFC = FeaturedContent(1, 1, Language(code = "eng", name = "English"), "Title", "Description", validUrl, validUrl)
  private val validNewFC = domain.FeaturedContent(Some(1), Some(1), LanguageTag("eng"), "Title", "Description", validUrl, validUrl)

  test("that valid new feature content is ok") {
    validationService.validateFeaturedContent(validNewFC) shouldBe Success(validNewFC)
  }

  test("that invalid new feature content is detected") {

    def assertFails(featuredContent: domain.FeaturedContent) = {
      withClue(s"Should not validate: $featuredContent") {
        validationService.validateFeaturedContent(featuredContent).isFailure shouldBe true
      }
    }

    assertFails(validNewFC.copy(title = ""))
    assertFails(validNewFC.copy(description = ""))
    assertFails(validNewFC.copy(link = ""))
    assertFails(validNewFC.copy(imageUrl = ""))

    assertFails(validNewFC.copy(title = "<script>Title</script>"))
    assertFails(validNewFC.copy(description = "<script>Description</script>"))
    assertFails(validNewFC.copy(link = "<a href=\"http://vg.no\">vg.no</a>"))
    assertFails(validNewFC.copy(imageUrl = "<img src=\"http://vg.no/logo.png\"/>"))

    assertFails(validNewFC.copy(link = "ftp://something"))
    assertFails(validNewFC.copy(imageUrl = "whatever"))
  }


  test("that valid updated feature content is ok") {
    validationService.validateUpdatedFeaturedContent(validUpdatedFC) shouldBe Success(validUpdatedFC)
  }

  test("that invalid updated feature content is detected") {

    def assertFails(featuredContent: FeaturedContent) = {
      withClue(s"Should not validate: $featuredContent") {
        validationService.validateUpdatedFeaturedContent(featuredContent).isFailure shouldBe true
      }
    }

    assertFails(validUpdatedFC.copy(title = ""))
    assertFails(validUpdatedFC.copy(description = ""))
    assertFails(validUpdatedFC.copy(link = ""))
    assertFails(validUpdatedFC.copy(imageUrl = ""))

    assertFails(validUpdatedFC.copy(title = "<script>Title</script>"))
    assertFails(validUpdatedFC.copy(description = "<script>Description</script>"))
    assertFails(validUpdatedFC.copy(link = "<a href=\"http://vg.no\">vg.no</a>"))
    assertFails(validUpdatedFC.copy(imageUrl = "<img src=\"http://vg.no/logo.png\"/>"))

    assertFails(validUpdatedFC.copy(link = "ftp://something"))
    assertFails(validUpdatedFC.copy(imageUrl = "whatever"))
  }

  test("that invalid contributor type returns error") {
    val validationMessage = validationService.validContributorType("invalid")
    validationMessage.isDefined should be (true)
  }

  test("that valid contributor type does not return validation message") {
    validationService.validContributorType(ContributorType.Illustrator.toString) should be (None)
  }

}
