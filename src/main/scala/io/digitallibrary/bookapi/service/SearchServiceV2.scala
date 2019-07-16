/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.search

import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchHits}
import com.sksamuel.elastic4s.searches.HighlightFieldDefinition
import com.sksamuel.elastic4s.searches.queries.term.{TermQueryDefinition, TermsQueryDefinition}
import com.sksamuel.elastic4s.searches.queries._
import com.sksamuel.elastic4s.searches.sort.{FieldSortDefinition, ScoreSortDefinition, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.ElasticClient
import io.digitallibrary.bookapi.model.api.{BookHit, BookHitV2, Error, GdlSearchException, LocalDateSerializer, ResultWindowTooLargeException, SearchResult, SearchResultV2}
import io.digitallibrary.bookapi.model.domain.{Paging, Sort}
import io.digitallibrary.bookapi.repository.TranslationRepository
import io.digitallibrary.bookapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}

import scala.util.{Failure, Success}

trait SearchServiceV2 {
  this: ElasticClient with ConverterService with IndexBuilderService with IndexService with TranslationRepository =>
  val searchServiceV2: SearchServiceV2

  class SearchServiceV2 extends LazyLogging {
    implicit val formats: Formats = DefaultFormats + LocalDateSerializer

    def searchWithQuery(languageTag: LanguageTag, query: Option[String], source: Option[String], paging: Paging, sort: Sort.Value): SearchResultV2 =
      executeSearch(boolDefinition = BoolQueryDefinition(), languageTag = Some(languageTag), query = query, categories = Seq(), readingLevel = None, source = source, paging = paging, sort = sort)

    def searchWithQueryForAllLanguages(query: Option[String], source: Option[String], paging: Paging, sort: Sort.Value): SearchResultV2 =
      executeSearch(boolDefinition = BoolQueryDefinition(), languageTag = None, query = query, categories = Seq(), readingLevel = None, source = source, paging = paging, sort = sort)

    def searchWithCategoryAndLevel(languageTag: LanguageTag, category: Option[String], readingLevel: Option[String], source: Option[String], paging: Paging, sort: Sort.Value): SearchResultV2 =
      executeSearch(boolDefinition = BoolQueryDefinition(), languageTag = Some(languageTag), query = None, categories = category.toSeq, readingLevel = readingLevel, source = source, paging = paging, sort = sort)

    def searchSimilar(languageTag: LanguageTag, bookId: Long, paging: Paging, sort: Sort.Value): SearchResultV2 = {
      val translation = unFlaggedTranslationsRepository.forBookIdAndLanguage(bookId, languageTag)
      translation match {
        case None => SearchResultV2(0, paging.page, paging.pageSize, Some(languageTag).map(converterService.toApiLanguage), Seq())
        case Some(trans) =>
          val moreLikeThisDefinition = MoreLikeThisQueryDefinition(Seq("readingLevel","language"),
            likeDocs = Seq(MoreLikeThisItem(BookApiProperties.searchIndex(languageTag), BookApiProperties.SearchDocument, trans.id.get.toString)),
            minDocFreq = Some(1), minTermFreq = Some(1), minShouldMatch = Some("100%"))
          executeSearch(boolDefinition = BoolQueryDefinition().must(moreLikeThisDefinition), languageTag = Some(languageTag), query = None, categories = trans.categories.map(_.name), readingLevel = None, source = None, paging = paging, sort = sort)
      }
    }

    private def executeSearch(boolDefinition: BoolQueryDefinition, languageTag: Option[LanguageTag], query: Option[String],
                              categories: Seq[String], readingLevel: Option[String], source: Option[String], paging: Paging, sort: Sort.Value): SearchResultV2 = {

      val (startAt, numResults) = getStartAtAndNumResults(paging.page, paging.pageSize)

      val requestedResultWindow = paging.page * numResults
      if (requestedResultWindow > BookApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${BookApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }
      val indexAndTypes = IndexAndTypes(languageTag.map(BookApiProperties.searchIndex).getOrElse(BookApiProperties.searchIndexPatternForAllLanguages()), Seq(BookApiProperties.SearchDocument))

      val queryDefinition = query match {
        case None => boolDefinition
        case Some(value) => boolDefinition.must(SimpleStringQueryDefinition(value.toLowerCase).field("description",1.0).field("title",1.4))
      }

      val categoryFilter = categories match {
        case head :: tail => Some(TermsQueryDefinition("categories.name", head :: tail))
        case _ => None
      }

      val levelFilter = readingLevel.map(TermQueryDefinition("readingLevel", _))
      val sourceFilter = source.map(TermQueryDefinition("source", _))

      val filteredSearch = queryDefinition.filter(Seq(categoryFilter, levelFilter, sourceFilter).flatten)

      val search = searchWithType(indexAndTypes)
        .size(numResults).from(startAt)
        .bool(filteredSearch)
        .sortBy(getSorting(sort))
        .highlighting(List(
          HighlightFieldDefinition("title", numOfFragments = Some(0)),
          HighlightFieldDefinition("description", numOfFragments = Some(0))))

      esClient.execute(search) match {
        case Success(response) => SearchResultV2(response.result.totalHits, paging.page, numResults, languageTag.map(converterService.toApiLanguage), getHits(response.result.hits))
        case Failure(failure: GdlSearchException) =>
          failure.getFailure.status match {
            case 404 => SearchResultV2(0, paging.page, numResults, languageTag.map(converterService.toApiLanguage), Seq())
            case _ => errorHandler(languageTag, Failure(failure))
          }
        case Failure(failure) => errorHandler(languageTag, Failure(failure))
      }
    }

    private def getSorting(sortDef: Sort.Value) = sortDef match {
      case (Sort.ByRelevance) => ScoreSortDefinition(order = SortOrder.DESC)
      case (Sort.ByIdAsc) => FieldSortDefinition("id", order = SortOrder.ASC)
      case (Sort.ByIdDesc) => FieldSortDefinition("id", order = SortOrder.DESC)
      case (Sort.ByTitleAsc) => FieldSortDefinition("title.sort", order = SortOrder.ASC)
      case (Sort.ByTitleDesc) => FieldSortDefinition("title.sort", order = SortOrder.DESC)
      case (Sort.ByArrivalDateAsc) => FieldSortDefinition("dateArrived", order = SortOrder.ASC)
      case (Sort.ByArrivalDateDesc) => FieldSortDefinition("dateArrived", order = SortOrder.DESC)
    }

    protected def getHits(hits: SearchHits): Seq[BookHitV2] = {
      hits.hits.toSeq.map(hit => getHit(hit))
    }

    private def getHit(hit: SearchHit): BookHitV2 = {
      val json:BookHitV2 = read[BookHitV2](hit.sourceAsString)
      val bookHit = (hit.highlightFragments("title"), hit.highlightFragments("description")) match {
        case (Seq(), Seq()) => json
        case (title::_, Seq()) => json.copy(highlightTitle = Some(title))
        case (Seq(), description::_) => json.copy(highlightDescription = Some(description))
        case (title::_, description::_) => json.copy(highlightTitle = Some(title), highlightDescription = Some(description))
      }
      bookHit
    }

    private def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val startAt = (page-1) * pageSize
      (startAt, pageSize)
    }

    private def errorHandler[T](languageTag: Option[LanguageTag], failure: Failure[T]) = {
      failure match {
        case Failure(e: GdlSearchException) =>
          logger.error(e.getFailure.error.reason)
          throw new ElasticsearchException(s"Unable to execute search in ${languageTag.map(BookApiProperties.searchIndex).getOrElse(BookApiProperties.searchIndexPatternForAllLanguages())}", e.getFailure.error.reason)
        case Failure(t: Throwable) => throw t
      }
    }
  }
}
