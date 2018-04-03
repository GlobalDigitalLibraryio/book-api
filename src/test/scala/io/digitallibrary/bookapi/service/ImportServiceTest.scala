package io.digitallibrary.bookapi.service

import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api.{Contributor, ValidationException}
import io.digitallibrary.bookapi.model.domain.Translation
import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import org.mockito.Mockito._
import scalikejdbc.DBSession

class ImportServiceTest extends UnitSuite with TestEnvironment {
  val service = new ImportService

  override def beforeEach = {
    resetMocks()
  }

  test("that validCategories returns Failure with validation messages for each invalid category") {
    val invalidCategory1 = api.Category(2, 1, "This is invalid")
    val invalidCategory2 = api.Category(3, 1, "This is also invalid")
    val book = TestData.Internal.DefaultInternalBook.copy(categories = Seq(invalidCategory1, invalidCategory2))

    when(categoryRepository.withName(anyString())(any[DBSession])).thenReturn(None)

    val result = service.validCategories(book)
    result should be a 'Failure

    val validationException = result.failed.get.asInstanceOf[ValidationException]
    validationException.errors.head.message should equal("This is invalid is not a valid category.")
    validationException.errors.last.message should equal("This is also invalid is not a valid category.")
  }

  test("that validCategories returns Success when all categories are valid") {
    val category1 = api.Category(2, 1, "category1")
    val category2 = api.Category(3, 1, "category2")
    val book = TestData.Internal.DefaultInternalBook.copy(categories = Seq(category1, category2))

    when(categoryRepository.withName(eqTo("category1"))(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultCategory.copy(name = "Category 1")))
    when(categoryRepository.withName(eqTo("category2"))(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultCategory.copy(name = "Category 1")))

    val result = service.validCategories(book)
    result should be a 'Success
  }


  test("that persistContributors adds persons with new names") {
    val existingPersonContributor = TestData.Api.DefaultContributor
    val nonExistingPersonContributor = TestData.Api.DefaultContributor.copy(name = "Does not exist")

    val contributors: Seq[Contributor] = Seq(existingPersonContributor, nonExistingPersonContributor)
    val translation: Translation = TestData.Domain.DefaultTranslation

    when(personRepository.withName(eqTo(existingPersonContributor.name))(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultPerson))
    when(personRepository.withName(eqTo(nonExistingPersonContributor.name))(any[DBSession])).thenReturn(None)

    when(personRepository.add(any[domain.Person])(any[DBSession])).thenReturn(TestData.Domain.DefaultPerson)
    when(contributorRepository.add(any[domain.Contributor])(any[DBSession])).thenReturn(TestData.Domain.DefaultContributor)

    val result = service.persistContributors(contributors, translation)
    result should be a 'Success
    result.get.size should be(2)

    verify(personRepository, times(1)).add(any[domain.Person])(any[DBSession])
    verify(contributorRepository, times(2)).add(any[domain.Contributor])(any[DBSession])
  }

  test("that persistContributorsUpdate removes contributors that no longer are part of book") {
    val translation = TestData.Domain.DefaultTranslation.copy(contributors = Seq(TestData.Domain.DefaultContributor))
    val book = TestData.Internal.DefaultInternalBook.copy(contributors = Seq())

    service.persistContributorsUpdate(translation, book)
    verify(contributorRepository).remove(any[domain.Contributor])(any[DBSession])
  }

  test("that persistContributorsUpdate adds contributors that are part of book") {
    val translation = TestData.Domain.DefaultTranslation.copy(contributors = Seq())
    val book = TestData.Internal.DefaultInternalBook.copy(contributors = Seq(TestData.Api.DefaultContributor))

    when(personRepository.withName(anyString())(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultPerson))
    when(contributorRepository.add(any[domain.Contributor])(any[DBSession])).thenReturn(TestData.Domain.DefaultContributor)

    service.persistContributorsUpdate(translation, book)
    verify(contributorRepository, never()).remove(any[domain.Contributor])(any[DBSession])
    verify(contributorRepository).add(any[domain.Contributor])(any[DBSession])
  }

  test("that persistChapterUpdates updates existing chapters") {
    val translation = TestData.Domain.DefaultTranslation
    val book = TestData.Internal.DefaultInternalBook.copy(chapters = Seq(TestData.Api.Chapter1))

    when(chapterRepository.forTranslationWithSeqNo(eqTo(translation.id.get), eqTo(TestData.Api.Chapter1.seqNo.toLong))(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultChapter))
    when(chapterRepository.updateChapter(any[domain.Chapter])(any[DBSession])).thenReturn(TestData.Domain.DefaultChapter)

    val result = service.persistChapterUpdates(book, translation)
    result should be a 'Success

    verify(chapterRepository).updateChapter(any[domain.Chapter])(any[DBSession])
  }

  test("that persistChapterUpdates adds new chapters") {
    val translation = TestData.Domain.DefaultTranslation
    val book = TestData.Internal.DefaultInternalBook.copy(chapters = Seq(TestData.Api.Chapter1))

    when(chapterRepository.forTranslationWithSeqNo(eqTo(translation.id.get), eqTo(TestData.Api.Chapter1.seqNo.toLong))(any[DBSession])).thenReturn(None)
    when(chapterRepository.updateChapter(any[domain.Chapter])(any[DBSession])).thenReturn(TestData.Domain.DefaultChapter)

    when(converterService.toDomainChapter(any[api.Chapter], any[Long])).thenReturn(TestData.Domain.DefaultChapter)
    when(chapterRepository.add(any[domain.Chapter])(any[DBSession])).thenReturn(TestData.Domain.DefaultChapter)

    val result = service.persistChapterUpdates(book, translation)
    result should be a 'Success

    verify(chapterRepository).add(any[domain.Chapter])(any[DBSession])
  }

  test("that persistPublisher creates a new publisher when no id") {
    val publisher = TestData.Domain.DefaultPublisher.copy(id = None, revision = None)
    when(publisherRepository.add(any[domain.Publisher])(any[DBSession])).thenReturn(TestData.Domain.DefaultPublisher)

    val result = service.persistPublisher(publisher)
    result should be a 'Success
    verify(publisherRepository).add(any[domain.Publisher])(any[DBSession])
  }

}
