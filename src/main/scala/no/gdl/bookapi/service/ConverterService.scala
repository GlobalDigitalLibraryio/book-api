/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import java.time.LocalDate
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.network.AuthUser
import no.gdl.bookapi.BookApiProperties.Domain
import no.gdl.bookapi.controller.NewFeaturedContent
import no.gdl.bookapi.integration.ImageApiClient
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.internal.{NewChapter, NewEducationalAlignment, NewTranslation}
import no.gdl.bookapi.model.crowdin.CrowdinFile
import no.gdl.bookapi.model.domain.{FileType, InTranslation, TranslationStatus}
import no.gdl.bookapi.{BookApiProperties, model}


trait ConverterService {
  this: ImageApiClient with ContentConverter =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {

    def toFeaturedContent(newFeaturedContent: NewFeaturedContent): domain.FeaturedContent = {
      domain.FeaturedContent(
        id = None,
        revision = None,
        language = LanguageTag(newFeaturedContent.language),
        title = newFeaturedContent.title,
        description = newFeaturedContent.description,
        imageUrl = newFeaturedContent.imageUrl,
        link = newFeaturedContent.link)
    }

    def toDomainChapter(newChapter: NewChapter, translationId: Long): domain.Chapter = domain.Chapter(
      id = None,
      revision = None,
      translationId = translationId,
      seqNo = newChapter.seqNo,
      title = newChapter.title,
      content = newChapter.content)


    def toDomainTranslation(newTranslation: NewTranslation, bookId: Long) = {
      domain.Translation(
        None,
        None,
        bookId,
        newTranslation.externalId,
        UUID.randomUUID().toString,
        newTranslation.title,
        newTranslation.about,
        newTranslation.numPages.map(_.toInt),
        LanguageTag(newTranslation.language),
        newTranslation.datePublished,
        newTranslation.dateCreated,
        categoryIds = Seq(),
        newTranslation.coverphoto.map(_.toLong),
        newTranslation.tags,
        newTranslation.isBasedOnUrl,
        newTranslation.educationalUse,
        newTranslation.educationalRole,
        eaId = None,
        newTranslation.timeRequired,
        newTranslation.typicalAgeRange,
        newTranslation.readingLevel,
        newTranslation.interactivityType,
        newTranslation.learningResourceType,
        newTranslation.accessibilityApi,
        newTranslation.accessibilityControl,
        newTranslation.accessibilityFeature,
        newTranslation.accessibilityHazard,
        newTranslation.dateArrived.getOrElse(LocalDate.now()),
        newTranslation.educationalAlignment.map(toDomainEducationalAlignment),
        chapters = Seq(),
        contributors = Seq(),
        categories = Seq()
      )
    }

    def toDomainEducationalAlignment(newEducationalAlignment: NewEducationalAlignment): domain.EducationalAlignment = {
      domain.EducationalAlignment(
        None,
        None,
        newEducationalAlignment.alignmentType,
        newEducationalAlignment.educationalFramework,
        newEducationalAlignment.targetDescription,
        newEducationalAlignment.targetName,
        newEducationalAlignment.targetUrl
      )
    }


    def toDomainLicense(license: api.License): domain.License = {
      domain.License(Option(license.id), Option(license.revision), license.name, license.description, license.url)
    }

    def toDomainPublisher(publisher: api.Publisher): domain.Publisher = {
      domain.Publisher(Option(publisher.id), Option(publisher.revision), publisher.name)
    }

    def toApiLicense(license: domain.License): api.License =
      api.License(license.id.get, license.revision.get, license.name, license.description, license.url)

    def toApiPublisher(publisher: domain.Publisher): api.Publisher = api.Publisher(publisher.id.get, publisher.revision.get, publisher.name)

    def toApiCategories(categories: Seq[domain.Category]): Seq[api.Category] =
      categories.map(c => api.Category(c.id.get, c.revision.get, c.name))

    def toApiContributors(contributors: Seq[domain.Contributor]): Seq[api.Contributor] =
      contributors.map(c => api.Contributor(c.id.get, c.revision.get, c.`type`, c.person.name))

    def toApiChapterSummary(chapters: Seq[domain.Chapter], bookId: Long, language: LanguageTag): Seq[api.ChapterSummary] = chapters.map(c => toApiChapterSummary(c, bookId, language))

    def toApiChapterSummary(chapter: domain.Chapter, bookId: Long, language: LanguageTag): api.ChapterSummary = api.ChapterSummary(
      chapter.id.get,
      chapter.seqNo,
      chapter.title,
      s"${Domain}${BookApiProperties.ApiPath}/${language.toString}/${bookId}/chapters/${chapter.id.get}")

    def toApiChapter(chapter: domain.Chapter): api.Chapter = api.Chapter(
      chapter.id.get,
      chapter.revision.get,
      chapter.seqNo,
      chapter.title,
      contentConverter.toApiContent(chapter.content))

    def toApiBook(translation: Option[domain.Translation], availableLanguages: Seq[LanguageTag], book: Option[domain.Book]): Option[api.Book] = {
      def toApiBookInternal(translation: domain.Translation, book: domain.Book, availableLanguages: Seq[LanguageTag]): api.Book = {
        model.api.Book(
          book.id.get,
          book.revision.get,
          translation.externalId,
          translation.uuid,
          translation.title,
          translation.about,
          None, // TODO in #155: Add from language if this is a translation
          toApiLanguage(translation.language),
          availableLanguages.map(toApiLanguage).sortBy(_.name),
          toApiLicense(book.license),
          toApiPublisher(book.publisher),
          translation.readingLevel,
          translation.typicalAgeRange,
          translation.educationalUse,
          translation.educationalRole,
          translation.timeRequired,
          translation.datePublished,
          translation.dateCreated,
          translation.dateArrived,
          toApiCategories(translation.categories),
          toApiCoverPhoto(translation.coverphoto),
          toApiDownloads(translation),
          translation.tags,
          toApiContributors(translation.contributors),
          toApiChapterSummary(translation.chapters, translation.bookId, translation.language),
          supportsTranslation = true // TODO in #155: Only set this to true if we support it
        )
      }

      for {
        b <- book
        t <- translation
        api <- Some(toApiBookInternal(t, b, availableLanguages))
      } yield api
    }

    def toApiDownloads(translation: domain.Translation): api.Downloads = {
      api.Downloads(
        epub = s"${BookApiProperties.CloudFrontBooks}/epub/${translation.language}/${translation.uuid}.epub", // TODO: #17 - Download EPub
        pdf = s"${BookApiProperties.CloudFrontBooks}/pdf/${translation.language}/${translation.uuid}.pdf") // TODO: #17 - Download PDF
    }

    def toApiCoverPhoto(imageIdOpt: Option[Long]): Option[api.CoverPhoto] = {
      imageIdOpt.flatMap(imageId =>
        imageApiClient.imageMetaWithId(imageId))
        .map(imageMeta => {
          val large = imageMeta.imageUrl
          val small = s"$large?width=200"
          api.CoverPhoto(large, small)
        })
    }

    def toApiLanguage(languageTag: LanguageTag): api.Language = {
      api.Language(languageTag.toString, languageTag.displayName)
    }

    def asDomainInTranslation(translationRequest: api.TranslateRequest, crowdinProjectId: String) = domain.InTranslation(
      id = None,
      revision = None,
      userIds = Seq(AuthUser.get.get),
      originalTranslationId = translationRequest.bookId,
      newTranslationId = None,
      fromLanguage = LanguageTag(translationRequest.fromLanguage),
      toLanguage = LanguageTag(translationRequest.toLanguage),
      crowdinToLanguage = translationRequest.toLanguage,
      crowdinProjectId = crowdinProjectId)

    def asDomainInTranslationFile(file: CrowdinFile, inTranslation: InTranslation) = domain.InTranslationFile(
        id = None,
        revision = None,
        inTranslationId = inTranslation.id.get,
        fileType = file.fileType,
        originalId = file.sourceId,
        filename = file.addedFile.name,
        crowdinFileId = file.addedFile.fileId.toString,
        translationStatus = TranslationStatus.IN_PROGRESS,
        etag = None)

  }
}
