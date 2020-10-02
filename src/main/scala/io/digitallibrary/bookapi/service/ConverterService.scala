/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import java.time.LocalDate
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.network.AuthUser
import io.digitallibrary.bookapi.BookApiProperties.Domain
import io.digitallibrary.bookapi.controller.NewFeaturedContent
import io.digitallibrary.bookapi.integration.{ImageApiClient, ImageVariant}
import io.digitallibrary.bookapi.integration.crowdin.CrowdinUtils
import io.digitallibrary.bookapi.model.{api, _}
import io.digitallibrary.bookapi.model.api.internal
import io.digitallibrary.bookapi.model.api.internal.{NewChapter, NewEducationalAlignment, NewTranslation}
import io.digitallibrary.bookapi.model.crowdin.CrowdinFile
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.{BookApiProperties, model}
import io.digitallibrary.license.model.License


trait ConverterService {
  this: ImageApiClient with ContentConverter =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def asDomainTranslateRequest(translateRequest: api.TranslateRequest, userId: String): TranslateRequest = domain.TranslateRequest(
      translateRequest.bookId,
      translateRequest.fromLanguage,
      translateRequest.toLanguage,
      Some(userId))


    def toBookForTranslation(book: api.Book): Option[api.BookForTranslation] = {
      book.translatedFrom.map(fromLanguage => {
        api.BookForTranslation(
          book.id,
          book.title,
          book.description,
          book.coverImage,
          book.chapters.map(chapter => {
            api.ChapterSummary(chapter.id, chapter.seqNo, chapter.title, s"${Domain}${BookApiProperties.TranslationsPath}/${fromLanguage.code}/${book.id}/chapters/${chapter.id}")
          }))
      })
    }

    def toDomainChapter(chapter: api.Chapter, translationId: Long): domain.Chapter = {
      domain.Chapter(
        id = None,
        revision = None,
        translationId = translationId,
        seqNo = chapter.seqNo,
        title = chapter.title,
        content = chapter.content,
        chapterType = ChapterType.valueOf(chapter.chapterType).get)
    }

    def mergeChapter(existing: domain.Chapter, replacement: api.Chapter): domain.Chapter = {
      existing.copy(
        seqNo = replacement.seqNo,
        title = replacement.title,
        content = replacement.content,
        chapterType = ChapterType.valueOf(replacement.chapterType).get
      )
    }

    def mergeTranslation(existingTranslation: Translation, newBook: internal.Book, categories: Seq[Category]): domain.Translation = {
      existingTranslation.copy(
        externalId = newBook.externalId,
        title = newBook.title,
        about = newBook.description,
        language = LanguageTag(newBook.language.code),
        translatedFrom = newBook.translatedFrom.map(tf => LanguageTag(tf.code)),
        datePublished = newBook.datePublished,
        dateCreated = newBook.dateCreated,
        categoryIds = categories.map(_.id.get),
        coverphoto = newBook.coverPhoto.map(_.imageApiId),
        tags = newBook.tags,
        educationalUse = newBook.educationalUse,
        educationalRole = newBook.educationalRole,
        timeRequired = newBook.timeRequired,
        typicalAgeRange = newBook.typicalAgeRange,
        readingLevel = newBook.readingLevel,
        dateArrived = newBook.dateArrived,
        publishingStatus = PublishingStatus.PUBLISHED,
        categories = categories,
        bookFormat = BookFormat.valueOfOrDefault(newBook.bookFormat),
        pageOrientation = PageOrientation.valueOfOrDefault(newBook.pageOrientation)
      )
    }

    def toDomainTranslation(newBook: internal.Book, persistedBook: Book, categories: Seq[Category]): domain.Translation = {
      domain.Translation(
        id = None,
        revision = None,
        bookId = persistedBook.id.get,
        externalId = newBook.externalId,
        uuid = UUID.randomUUID().toString,
        title = newBook.title,
        about = newBook.description,
        numPages = None,
        language = LanguageTag(newBook.language.code),
        translatedFrom = newBook.translatedFrom.map(tf => LanguageTag(tf.code)),
        datePublished = newBook.datePublished,
        dateCreated = newBook.dateCreated,
        categoryIds = categories.map(_.id.get),
        coverphoto = newBook.coverPhoto.map(_.imageApiId),
        tags = newBook.tags,
        isBasedOnUrl = None,
        educationalUse = newBook.educationalUse,
        educationalRole = newBook.educationalRole,
        eaId = None,
        timeRequired = newBook.timeRequired,
        typicalAgeRange = newBook.typicalAgeRange,
        readingLevel = newBook.readingLevel,
        interactivityType = None,
        learningResourceType = None,
        accessibilityApi = None,
        accessibilityControl = None,
        accessibilityFeature = None,
        accessibilityHazard = None,
        dateArrived = newBook.dateArrived,
        publishingStatus = PublishingStatus.PUBLISHED,
        translationStatus = None,
        educationalAlignment = None,
        chapters = Seq(),
        contributors = Seq(),
        categories = categories,
        bookFormat = BookFormat.valueOfOrDefault(newBook.bookFormat),
        pageOrientation = PageOrientation.valueOfOrDefault(newBook.pageOrientation),
        additionalInformation = newBook.additionalInformation,
        officiallyApproved = newBook.officiallyApproved.getOrElse(default = false)
      )
    }

    def toFeaturedContent(newFeaturedContent: NewFeaturedContent, category: Option[Category]): domain.FeaturedContent = {
      domain.FeaturedContent(
        id = None,
        revision = None,
        language = LanguageTag(newFeaturedContent.language),
        title = newFeaturedContent.title,
        description = newFeaturedContent.description,
        imageUrl = newFeaturedContent.imageUrl,
        link = newFeaturedContent.link,
        categoryId = category.flatMap(_.id)
      )
    }

    def toDomainChapter(newChapter: NewChapter, translationId: Long): domain.Chapter = domain.Chapter(
      id = None,
      revision = None,
      translationId = translationId,
      seqNo = newChapter.seqNo,
      title = newChapter.title,
      content = newChapter.content,
      chapterType = ChapterType.valueOfOrDefault(newChapter.chapterType))

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
        newTranslation.translatedFrom.map(LanguageTag(_)),
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
        PublishingStatus.PUBLISHED,
        None,
        newTranslation.educationalAlignment.map(toDomainEducationalAlignment),
        chapters = Seq(),
        contributors = Seq(),
        categories = Seq(),
        bookFormat = BookFormat.valueOfOrDefault(newTranslation.bookFormat),
        pageOrientation = PageOrientation.valueOfOrDefault(newTranslation.pageOrientation),
        additionalInformation = newTranslation.additionalInformation
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

    def toDomainPublisher(publisher: api.Publisher): domain.Publisher = {
      domain.Publisher(Option(publisher.id), Option(publisher.revision), publisher.name)
    }

    def toApiLicense(license: License): api.License =
      api.License(license.name, Some(license.description), Some(license.url))

    def toApiPublisher(publisher: domain.Publisher): api.Publisher = api.Publisher(publisher.id.get, publisher.revision.get, publisher.name)

    def toApiCategory(category: domain.Category): api.Category =
      api.Category(category.id.get, category.revision.get, category.name)

    def toApiCategories(categories: Seq[domain.Category]): Seq[api.Category] =
      categories.map(toApiCategory)

    def toApiContributors(contributors: Seq[domain.Contributor]): Seq[api.Contributor] =
      contributors.map(c => api.Contributor(c.id.get, c.revision.get, c.`type`.toString, c.person.name))

    def toApiChapterSummary(chapters: Seq[domain.Chapter], bookId: Long, language: LanguageTag): Seq[api.ChapterSummary] = chapters.map(c => toApiChapterSummary(c, bookId, language))

    def toApiChapterSummary(chapter: domain.Chapter, bookId: Long, language: LanguageTag): api.ChapterSummary = api.ChapterSummary(
      chapter.id.get,
      chapter.seqNo,
      chapter.title,
      s"${Domain}${BookApiProperties.ApiPath}/${language.toString}/${bookId}/chapters/${chapter.id.get}")

    def toApiChapter(chapter: domain.Chapter, convertContent: Boolean = true): api.Chapter = {
      val apiContent = contentConverter.toApiContent(chapter.content)
      api.Chapter(
        chapter.id.get,
        chapter.revision.get,
        chapter.seqNo,
        chapter.title,
        if (convertContent) apiContent.content else chapter.content,
        chapter.chapterType.toString, apiContent.images)
    }

    def toInternalApiBook(translation: domain.Translation, availableLanguages: Seq[LanguageTag], book: domain.Book): api.internal.Book = {
      model.api.internal.Book(
        id = book.id.get,
        revision = book.revision.get,
        externalId = translation.externalId,
        uuid = translation.uuid,
        title = translation.title,
        description = translation.about,
        translatedFrom = translation.translatedFrom.map(toApiLanguage),
        language = toApiLanguage(translation.language),
        availableLanguages = availableLanguages.map(toApiLanguage).sortBy(_.name),
        license = toApiLicense(book.license),
        publisher = toApiPublisher(book.publisher),
        readingLevel = translation.readingLevel,
        typicalAgeRange = translation.typicalAgeRange,
        educationalUse = translation.educationalUse,
        educationalRole = translation.educationalRole,
        timeRequired = translation.timeRequired,
        datePublished = translation.datePublished,
        dateCreated = translation.dateCreated,
        dateArrived = translation.dateArrived,
        categories = toApiCategories(translation.categories),
        coverPhoto = toApiInternalCoverPhoto(translation.coverphoto),
        downloads = toApiDownloads(translation),
        tags = translation.tags,
        contributors = toApiContributors(translation.contributors),
        chapters = translation.chapters.map(toApiChapter(_, convertContent = false)),
        supportsTranslation = BookApiProperties.supportsTranslationFrom(translation.language) && translation.bookFormat.equals(BookFormat.HTML),
        bookFormat = translation.bookFormat.toString,
        source = book.source,
        pageOrientation = translation.pageOrientation.toString,
        additionalInformation = translation.additionalInformation,
        officiallyApproved = Some(translation.officiallyApproved))
    }

    def toApiBook(translation: domain.Translation, availableLanguages: Seq[LanguageTag], book: domain.Book): api.Book = {
      model.api.Book(
        id = book.id.get,
        revision = translation.revision.get,
        externalId = translation.externalId,
        uuid = translation.uuid,
        title = translation.title,
        description = translation.about,
        translatedFrom = translation.translatedFrom.map(toApiLanguage),
        language = toApiLanguage(translation.language),
        availableLanguages = availableLanguages.map(toApiLanguage).sortBy(_.name),
        license = toApiLicense(book.license),
        publisher = toApiPublisher(book.publisher),
        readingLevel = translation.readingLevel,
        typicalAgeRange = translation.typicalAgeRange,
        educationalUse = translation.educationalUse,
        educationalRole = translation.educationalRole,
        timeRequired = translation.timeRequired,
        datePublished = translation.datePublished,
        dateCreated = translation.dateCreated,
        dateArrived = translation.dateArrived,
        categories = toApiCategories(translation.categories),
        coverImage = toApiCoverImage(translation.coverphoto),
        downloads = toApiDownloads(translation),
        tags = translation.tags,
        contributors = toApiContributors(translation.contributors),
        chapters = toApiChapterSummary(translation.chapters, translation.bookId, translation.language),
        supportsTranslation = BookApiProperties.supportsTranslationFrom(translation.language) && translation.bookFormat.equals(BookFormat.HTML),
        bookFormat = translation.bookFormat.toString,
        pageOrientation = translation.pageOrientation.toString,
        source = book.source,
        publishingStatus = translation.publishingStatus.toString,
        translationStatus = translation.translationStatus.map(_.toString),
        additionalInformation = translation.additionalInformation,
        officiallyApproved = Some(translation.officiallyApproved))
    }

    def toApiBookHit(translation: Option[domain.Translation], availableLanguages: Option[Seq[LanguageTag]], book: Option[domain.Book]): Option[api.BookHit] = {
      def toApiBookHitInternal(translation: domain.Translation, availableLanguages: Seq[LanguageTag], book: domain.Book): api.BookHit = {
        model.api.BookHit(
          id = book.id.get,
          title = translation.title,
          description = translation.about,
          language = toApiLanguage(translation.language),
          availableLanguages = availableLanguages.map(toApiLanguage).sortBy(_.name),
          readingLevel = translation.readingLevel,
          categories = toApiCategories(translation.categories),
          coverImage = toApiCoverImage(translation.coverphoto),
          dateArrived = translation.dateArrived,
          source = book.source,
          highlightTitle = None,
          highlightDescription = None,
          license = Some(toApiLicense(book.license))
        )
      }

      for {
        b <- book
        al <- availableLanguages
        t <- translation
        api <- Some(toApiBookHitInternal(t, al, b))
      } yield api
    }

    def toApiMyBook(inTranslation: InTranslation, translation: domain.Translation, availableLanguages: Seq[LanguageTag], book: api.Book): api.MyBook = {
      api.MyBook(
        id = book.id,
        revision = book.revision,
        title = translation.title,
        translatedFrom = translation.translatedFrom.map(toApiLanguage),
        translatedTo = toApiLanguage(translation.language),
        publisher = book.publisher,
        coverImage = toApiCoverImage(translation.coverphoto),
        readingLevel = book.readingLevel,
        synchronizeUrl = s"${BookApiProperties.Domain}${BookApiProperties.TranslationsPath}/synchronized/${inTranslation.id.get}",
        crowdinUrl = CrowdinUtils.crowdinUrlToBook(book, inTranslation.crowdinProjectId, inTranslation.crowdinToLanguage))
    }


    def toApiDownloads(translation: domain.Translation): api.Downloads = {
      translation.bookFormat match {
        case BookFormat.HTML => api.Downloads(
          epub = Some(s"${BookApiProperties.CloudFrontBooks}/epub/${translation.language}/${translation.uuid}.epub"),
          pdf = Some(s"${BookApiProperties.CloudFrontBooks}/pdf/${translation.language}/${translation.uuid}.pdf"))
        case BookFormat.PDF => api.Downloads(pdf = Some(s"${BookApiProperties.CloudFrontBooks}/pdf/${translation.language}/${translation.uuid}.pdf"), epub = None)
      }
    }

    def asApiVariant(variant: ImageVariant): api.ImageVariant = {
      api.ImageVariant(variant.ratio, variant.x, variant.y, variant.width, variant.height)
    }

    def toApiCoverImage(imageIdOpt: Option[Long]): Option[api.CoverImage] = {
      imageIdOpt.flatMap(imageId =>
        imageApiClient.imageMetaWithId(imageId))
        .map(imageMeta => {
          val variants = imageMeta.imageVariants.map(x => x.map(entry => entry._1 -> asApiVariant(entry._2)))
          api.CoverImage(url = imageMeta.imageUrl, alttext = imageMeta.alttext.map(_.alttext), imageId = imageMeta.id, variants = variants)
        })
    }

    def toApiInternalCoverPhoto(imageIdOpt: Option[Long]): Option[api.internal.CoverPhoto] = imageIdOpt.map(api.internal.CoverPhoto)

    def toApiLanguage(languageTag: LanguageTag): api.Language = {
      api.Language(languageTag.toString, languageTag.localDisplayName.getOrElse(languageTag.displayName), if(languageTag.isRightToLeft) Some(languageTag.isRightToLeft) else None)
    }

    def asDomainInTranslation(translationRequest: domain.TranslateRequest, newTranslation: domain.Translation, crowdinProjectId: String) = domain.InTranslation(
      id = None,
      revision = None,
      userIds = Seq(translationRequest.userId).flatten,
      originalTranslationId = translationRequest.bookId,
      newTranslationId = newTranslation.id,
      fromLanguage = LanguageTag(translationRequest.fromLanguage),
      toLanguage = LanguageTag(translationRequest.toLanguage),
      crowdinToLanguage = translationRequest.toLanguage,
      crowdinProjectId = crowdinProjectId)

    def asDomainInTranslationFile(file: CrowdinFile, inTranslation: InTranslation) = domain.InTranslationFile(
        id = None,
        revision = None,
        inTranslationId = inTranslation.id.get,
        fileType = file.fileType,
        newChapterId = file.sourceId,
        seqNo = file.seqNo,
        filename = file.addedFile.name,
        crowdinFileId = file.addedFile.fileId.toString,
        translationStatus = if (file.addedFile.strings > 0)  TranslationStatus.IN_PROGRESS else TranslationStatus.PROOFREAD,
        etag = None)

  }
}
