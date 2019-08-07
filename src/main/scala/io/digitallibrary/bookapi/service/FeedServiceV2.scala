package io.digitallibrary.bookapi.service

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.OpdsLanguageParam
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.{api, domain}
import io.digitallibrary.bookapi.model.domain.{Paging, PublishingStatus, Sort}
import io.digitallibrary.bookapi.repository.{BookRepository, FeedRepository, TranslationRepository}
import io.digitallibrary.bookapi.service.search.SearchServiceV2
import io.digitallibrary.language.model.LanguageTag

trait FeedServiceV2 {
  this: FeedRepository with TranslationRepository with BookRepository with ReadServiceV2 with ConverterService with SearchServiceV2 with FeedLocalizationService =>
  val feedServiceV2: FeedServiceV2

  sealed trait PagingStatusV2
  case class OnlyOnePageV2(currentPaging: Paging) extends PagingStatusV2
  case class MoreAheadV2(currentPaging: Paging, lastPage: Int) extends PagingStatusV2
  case class MoreBeforeV2(currentPaging: Paging) extends PagingStatusV2
  case class MoreInBothDirectionsV2(currentPaging: Paging, lastPage: Int) extends PagingStatusV2

  sealed trait FeedTypeV2
  case class RootFeedV2(language: LanguageTag) extends FeedTypeV2
  case class CategoryFeedV2(language: LanguageTag, category: String) extends FeedTypeV2
  case class LevelFeedV2(language: LanguageTag, category: String, level: String) extends FeedTypeV2


  class FeedServiceV2 extends LazyLogging {
    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    def feedForUrl(url: String, feedType: FeedTypeV2, books: => Seq[FeedEntryV2]): Option[api.FeedV2] = {

      val facets = feedType match {
        case RootFeedV2(language) =>
          facetsForLanguages(language) ++
            facetsForCategories(language, currentCategory = None)
        case CategoryFeedV2(language, category) =>
          facetsForLanguages(language) ++
            facetsForCategories(language, Some(category)) ++
            facetsForReadingLevels(currentLanguage = language, currentCategory = category, None)
        case LevelFeedV2(language, category, level) =>
          facetsForLanguages(language) ++
            facetsForCategories(language, Some(category)) ++
            facetsForReadingLevels(currentLanguage = language, currentCategory = category, Some(level))
      }

      feedRepository.forUrl(url.replace(BookApiProperties.OpdsPath,"")).map(feedDefinition => {
        api.FeedV2(
          feedDefinition = api.FeedDefinition(
            feedDefinition.id.get,
            feedDefinition.revision.get,
            s"${BookApiProperties.CloudFrontOpds}${feedDefinition.url}",
            feedDefinition.uuid),
          title = feedDefinition.title,
          description = feedDefinition.description,
          rel = Some("self"),
          updated = feedDefinition.updated,
          content = books,
          facets = facets)
      })
    }

    def facetsForLanguages(currentLanguage: LanguageTag): Seq[Facet] = {
      readServiceV2.listAvailablePublishedLanguagesAsLanguageTags.sortBy(_.toString).map(lang => Facet(
        href = s"${
          BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsRootUrl
          .replace(BookApiProperties.OpdsLanguageParam, lang.toString)}",
        title = s"${lang.localDisplayName.getOrElse(lang.displayName)}",
        group = "Languages",
        isActive = lang == currentLanguage))
    }

    def facetsForCategories(currentLanguage: LanguageTag, currentCategory: Option[String]): Seq[Facet] = {
      val localization = feedLocalizationService.localizationFor(currentLanguage)
      readServiceV2.listAvailablePublishedCategoriesForLanguage(currentLanguage).keys.toList.sortWith(categorySort).map(category => Facet(
        href = s"${BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsCategoryUrl
          .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
          .replace(BookApiProperties.OpdsCategoryParam, category.name)}",
        title = localization.categoryTitle(category.name),
        group = "Category",
        isActive = currentCategory.contains(category.name)
      )
      )
    }

    def categorySort(c1: domain.Category, c2: domain.Category): Boolean = {
      if (c1.name == "library_books") {
        true
      } else {
        c1.name.compareTo(c2.name) < 0
      }
    }

    def levelOrder(level: String): Int = {
      level match {
        case "decodable" => 0
        case "read-aloud" => 100
        case "root" => 101
        case _ => level.toInt
      }
    }

    def facetsForReadingLevels(currentLanguage: LanguageTag, currentCategory: String, currentReadingLevel: Option[String]): Seq[Facet] = {
      val localization = feedLocalizationService.localizationFor(currentLanguage)
      val group = "Selection"
      readServiceV2.listAvailablePublishedLevelsForLanguage(Some(currentLanguage), Some(currentCategory))
        .sortBy(levelOrder).map(readingLevel =>
        Facet(
          href = s"${
            BookApiProperties.CloudFrontOpds
          }${
            BookApiProperties.OpdsCategoryAndLevelUrl
              .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
              .replace(BookApiProperties.OpdsCategoryParam, currentCategory)
              .replace(BookApiProperties.OpdsLevelParam, readingLevel)
          }",
          title = localization.levelTitle(readingLevel),
          group = group,
          isActive = currentReadingLevel.contains(readingLevel))
      ) ++
        Seq(Facet(
          href = s"${
            BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsCategoryUrl
            .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
            .replace(BookApiProperties.OpdsCategoryParam, currentCategory)
          }",
          title = s"New arrivals",
          group = group,
          isActive = currentReadingLevel.isEmpty))
    }

    def allEntries(language: LanguageTag, paging: Paging): (PagingStatusV2, Seq[FeedEntryV2]) = {

      val searchResult = {
        searchServiceV2.searchWithCategoryAndLevel(
          languageTag = language,
          category = None,
          readingLevel = None,
          source = None,
          paging = paging,
          sort = Sort.ByArrivalDateDesc)
      }

      (searchResultToPagingStatus(searchResult, paging), searchResultsToFeedEntries(searchResult.results, language))
    }

    def searchResultsToFeedEntries(bookHits: Seq[BookHitV2], language: LanguageTag): Seq[FeedEntryV2] = {
      for {
        bookHit <- bookHits
        book <- fromHit(bookHit, feedLocalizationService.localizationFor(language))
      } yield api.FeedEntryV2(book)
    }

    def fromHit(bookHit: BookHitV2, feedLocalization: FeedLocalization): Option[BookV2] = {
      for {
        translation <- unFlaggedTranslationsRepository.forBookIdAndLanguage(bookHit.id, LanguageTag(bookHit.language.code))
        book <- bookRepository.withId(bookHit.id)
        apiBook = converterService.toApiBookV2(translation, unFlaggedTranslationsRepository.languagesFor(bookHit.id), book)
        apiBookWithLocalizedReadingLevel = apiBook.copy(readingLevel = apiBook.readingLevel.map(feedLocalization.levelTitle))
      } yield apiBookWithLocalizedReadingLevel
    }

    def entriesForLanguageAndCategory(language: LanguageTag, category: String, paging: Paging): (PagingStatusV2, Seq[FeedEntryV2]) = {
      val searchResult = searchServiceV2.searchWithCategoryAndLevel(
        languageTag = language,
        category = Some(category),
        readingLevel = None,
        source = None,
        paging = paging,
        sort = Sort.ByTitleAsc)

      (searchResultToPagingStatus(searchResult, paging), searchResultsToFeedEntries(searchResult.results, language))
    }

    def entriesForLanguageCategoryAndLevel(language: LanguageTag, category: String, level: String, paging: Paging): (PagingStatusV2, Seq[FeedEntryV2]) = {
      val searchResult = searchServiceV2.searchWithCategoryAndLevel(
        languageTag = language,
        category = Some(category),
        readingLevel = Some(level),
        source = None,
        paging = paging,
        sort = Sort.ByTitleAsc)

      (searchResultToPagingStatus(searchResult, paging), searchResultsToFeedEntries(searchResult.results, language))
    }

    def searchResultToPagingStatus(searchResult: SearchResultV2, paging: Paging): PagingStatusV2 = {
      if (searchResult.totalCount > paging.pageSize) {
        val lastPage = Math.round(Math.ceil(searchResult.totalCount.toFloat / paging.pageSize)).toInt
        if (paging.page == lastPage) {
          MoreBeforeV2(currentPaging = paging)
        } else if (paging.page > 1) {
          MoreInBothDirectionsV2(currentPaging = paging, lastPage = lastPage)
        } else {
          MoreAheadV2(currentPaging = paging, lastPage = lastPage)
        }
      } else {
        OnlyOnePageV2(currentPaging = paging)
      }
    }

    def generateFeeds(): Seq[api.FeedDefinition] = {
      val feeds = calculateFeeds.map(createOrUpdateFeed)
      feeds.map(feed => api.FeedDefinition(feed.id.get, feed.revision.get, feed.url, feed.uuid))
    }

    def calculateFeeds: Seq[domain.Feed] = {
      val feeds = for {
        language <- unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)
        localization = feedLocalizationService.localizationFor(language)
        (category: domain.Category, levels: Set[String]) <- unFlaggedTranslationsRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, language)
        categoryFeed = domain.Feed(
          id = None,
          revision = None,
          url = BookApiProperties.OpdsCategoryUrl
            .replace(OpdsLanguageParam, language.toString)
            .replace(BookApiProperties.OpdsCategoryParam, category.name),
          uuid = UUID.randomUUID().toString,
          title = localization.categoriesTitle,
          description = Some(localization.categoriesDescription),
          updated = ZonedDateTime.now()
        )
        level <- levels
        feed <- Seq(
          domain.Feed(
            id = None,
            revision = None,
            url = BookApiProperties.OpdsRootUrl.replace(OpdsLanguageParam, language.toString),
            uuid = UUID.randomUUID().toString,
            title = localization.rootTitle,
            description = None,
            updated = ZonedDateTime.now()
          ),

          categoryFeed,

          domain.Feed(
            id = None,
            revision = None,
            url = BookApiProperties.OpdsCategoryAndLevelUrl
              .replace(OpdsLanguageParam, language.toString)
              .replace(BookApiProperties.OpdsCategoryParam, category.name)
              .replace(BookApiProperties.OpdsLevelParam, level),
            uuid = UUID.randomUUID().toString,
            title = localization.levelTitle(level),
            description = Some(localization.levelDescription),
            updated = ZonedDateTime.now()
          ))

      } yield feed
      val defaultRootFeed =
        domain.Feed(
          id = None,
          revision = None,
          url = BookApiProperties.OpdsRootDefaultLanguageUrl,
          uuid = UUID.randomUUID().toString,
          title = feedLocalizationService.localizationFor(LanguageTag("eng")).rootTitle,
          description = None,
          updated = ZonedDateTime.now()
        )
      defaultRootFeed +: feeds
    }

    def createOrUpdateFeed(feed: domain.Feed): domain.Feed = {
      feedRepository.addOrUpdate(feed)
    }
  }
}
