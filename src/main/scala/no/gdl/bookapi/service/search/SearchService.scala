/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchHits
import com.sksamuel.elastic4s.searches.queries.term.TermQueryDefinition
import com.sksamuel.elastic4s.searches.queries.{BoolQueryDefinition, QueryStringQueryDefinition}
import com.sksamuel.elastic4s.searches.sort.{FieldSortDefinition, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.integration.ElasticClient
import no.gdl.bookapi.model.api.{Book, Error, GdlSearchException, LocalDateSerializer, ResultWindowTooLargeException, SearchResult}
import no.gdl.bookapi.model.domain.{Paging, Sort}
import no.gdl.bookapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization.read

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


trait SearchService {
  this: ElasticClient with ConverterService with IndexBuilderService with IndexService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    implicit val formats: Formats = DefaultFormats + LocalDateSerializer

    def searchWithQuery(languageTag: LanguageTag, query: Option[String], paging: Paging, sort: Sort.Value): SearchResult =
      executeSearch(BoolQueryDefinition(), languageTag, query, None, paging, sort)

    def searchWithLevelAndStatus(languageTag: LanguageTag, readingLevel: Option[String],  paging: Paging, sort: Sort.Value): SearchResult =
      executeSearch(BoolQueryDefinition(), languageTag, None, readingLevel, paging, sort)

    private def executeSearch(boolDefinition: BoolQueryDefinition, languageTag: LanguageTag, query: Option[String], readingLevel: Option[String], paging: Paging, sort: Sort.Value) = {

      val (startAt, numResults) = getStartAtAndNumResults(paging.page, paging.pageSize)

      val requestedResultWindow = paging.page * numResults
      if (requestedResultWindow > BookApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${BookApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }
      val indexAndTypes = IndexAndTypes(BookApiProperties.searchIndex(languageTag), Seq(BookApiProperties.SearchDocument))

      val queryDefinition = query match {
        case None => boolDefinition
        case Some(value) => boolDefinition.should(QueryStringQueryDefinition(value).field("title").field("description"))
      }

      val levelDefinition = readingLevel match {
        case None => queryDefinition
        case Some(level) => queryDefinition.filter(Seq(TermQueryDefinition("readingLevel", level)))
      }

      val search = searchWithType(indexAndTypes)
        .size(numResults).from(startAt)
        .bool(levelDefinition)
        .sortBy(getSorting(sortDef = sort))

      esClient.execute(
        search) match {
        case Success(response) => SearchResult(response.result.totalHits, paging.page, numResults, converterService.toApiLanguage(languageTag), getHits(response.result.hits, languageTag))
        case Failure(failure) => errorHandler(languageTag, Failure(failure))
      }
    }

    private def getSorting(sortDef: Sort.Value) = sortDef match {
      case (Sort.ByIdAsc) => FieldSortDefinition("id", order = SortOrder.ASC)
      case (Sort.ByIdDesc) => FieldSortDefinition("id", order = SortOrder.DESC)
      case (Sort.ByTitleAsc) => FieldSortDefinition("title", order = SortOrder.ASC)
      case (Sort.ByTitleDesc) => FieldSortDefinition("title", order = SortOrder.DESC)
      case (Sort.ByArrivalDateAsc) => FieldSortDefinition("dateArrived", order = SortOrder.ASC)
      case (Sort.ByArrivalDateDesc) => FieldSortDefinition("dateArrived", order = SortOrder.DESC)
    }

    private def getHits(hits: SearchHits, language: LanguageTag): Seq[Book] = {
      hits.hits.iterator.toSeq.map(hit => hitAsApiBook(hit.sourceAsString, language))
    }

    private def hitAsApiBook(hit: String, language: LanguageTag): Book = {
      read[Book](hit)
    }

    private def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val startAt = (page-1) * pageSize
      (startAt, pageSize)
    }

    private def errorHandler[T](languageTag: LanguageTag, failure: Failure[T]) = {
      failure match {
        case Failure(e: GdlSearchException) =>
          e.getFailure.status match {
            case 404 =>
              logger.error(s"Index ${BookApiProperties.searchIndex(languageTag)} not found. Scheduling a reindex.")
              scheduleIndexDocuments(languageTag)
              throw new IndexNotFoundException(s"Index ${BookApiProperties.searchIndex(languageTag)} not found. Scheduling a reindex")
            case _ =>
              logger.error(e.getFailure.error.reason)
              throw new ElasticsearchException(s"Unable to execute search in ${BookApiProperties.searchIndex(languageTag)}", e.getFailure.error.reason)
          }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments(languageTag: LanguageTag): Unit = {
      val f = Future {
        indexBuilderService.indexDocumentsForLanguage(languageTag)
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }


}
