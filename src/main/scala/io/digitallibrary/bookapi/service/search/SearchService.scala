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
import com.sksamuel.elastic4s.searches.queries.term.TermQueryDefinition
import com.sksamuel.elastic4s.searches.queries.{BoolQueryDefinition, MoreLikeThisItem, MoreLikeThisQueryDefinition, QueryStringQueryDefinition}
import com.sksamuel.elastic4s.searches.sort.{FieldSortDefinition, ScoreSortDefinition, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.ElasticClient
import io.digitallibrary.bookapi.model.api.{BookHit, Error, GdlSearchException, LocalDateSerializer, ResultWindowTooLargeException, SearchResult}
import io.digitallibrary.bookapi.model.domain.{Paging, Sort}
import io.digitallibrary.bookapi.repository.TranslationRepository
import io.digitallibrary.bookapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}

import scala.util.{Failure, Success}

trait SearchService {
  this: ElasticClient with ConverterService with IndexBuilderService with IndexService with TranslationRepository =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    implicit val formats: Formats = DefaultFormats + LocalDateSerializer

    def searchWithQuery(languageTag: LanguageTag, query: Option[String], paging: Paging, sort: Sort.Value): SearchResult =
      executeSearch(BoolQueryDefinition(), languageTag, query, None, paging, sort)

    def searchWithLevel(languageTag: LanguageTag, readingLevel: Option[String], paging: Paging, sort: Sort.Value): SearchResult =
      executeSearch(BoolQueryDefinition(), languageTag, None, readingLevel, paging, sort)

    def searchSimilar(languageTag: LanguageTag, bookId: Long, paging: Paging, sort: Sort.Value): SearchResult = {
      val translation = translationRepository.forBookIdAndLanguage(bookId, languageTag)
      translation match {
        case None => SearchResult(0, paging.page, paging.pageSize, converterService.toApiLanguage(languageTag), Seq())
        case Some(trans) =>
          val moreLikeThisDefinition = MoreLikeThisQueryDefinition(Seq("readingLevel","language"),
            likeDocs = Seq(MoreLikeThisItem(BookApiProperties.searchIndex(languageTag), BookApiProperties.SearchDocument, trans.id.get.toString)),
            minDocFreq = Some(1), minTermFreq = Some(1), minShouldMatch = Some("100%"))
          executeSearch(BoolQueryDefinition().should(moreLikeThisDefinition), languageTag, None, None, paging, sort)
      }
    }

    private def executeSearch(boolDefinition: BoolQueryDefinition, languageTag: LanguageTag, query: Option[String],
                                              readingLevel: Option[String], paging: Paging, sort: Sort.Value): SearchResult = {

      val (startAt, numResults) = getStartAtAndNumResults(paging.page, paging.pageSize)

      val requestedResultWindow = paging.page * numResults
      if (requestedResultWindow > BookApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${BookApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }
      val indexAndTypes = IndexAndTypes(BookApiProperties.searchIndex(languageTag), Seq(BookApiProperties.SearchDocument))

      val queryDefinition = query match {
        case None => boolDefinition
        case Some(value) => boolDefinition.should(QueryStringQueryDefinition(value.toLowerCase).field("description",1.0).field("title",1.4))
      }

      val levelDefinition = readingLevel match {
        case None => queryDefinition
        case Some(level) => queryDefinition.filter(Seq(TermQueryDefinition("readingLevel", level)))
      }

      val search = searchWithType(indexAndTypes)
        .size(numResults).from(startAt)
        .bool(levelDefinition)
        .sortBy(getSorting(sort))
        .highlighting(List(
          HighlightFieldDefinition("title", numOfFragments = Some(0)),
          HighlightFieldDefinition("description", numOfFragments = Some(0))))

      esClient.execute(search) match {
        case Success(response) => SearchResult(response.result.totalHits, paging.page, numResults, converterService.toApiLanguage(languageTag), getHits(response.result.hits, languageTag))
        case Failure(failure: GdlSearchException) =>
          failure.getFailure.status match {
            case 404 => SearchResult(0, paging.page, numResults, converterService.toApiLanguage(languageTag), Seq())
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

    private def getHits(hits: SearchHits, language: LanguageTag): Seq[BookHit] = {
      hits.hits.toSeq.map(hit => getHit(hit))
    }

    private def getHit(hit: SearchHit): BookHit = {
      val json = read[BookHit](hit.sourceAsString)
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

    private def errorHandler[T](languageTag: LanguageTag, failure: Failure[T]) = {
      failure match {
        case Failure(e: GdlSearchException) =>
          logger.error(e.getFailure.error.reason)
          throw new ElasticsearchException(s"Unable to execute search in ${BookApiProperties.searchIndex(languageTag)}", e.getFailure.error.reason)
        case Failure(t: Throwable) => throw t
      }
    }
  }
}
