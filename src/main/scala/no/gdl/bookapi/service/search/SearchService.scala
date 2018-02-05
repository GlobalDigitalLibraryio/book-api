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
import com.sksamuel.elastic4s.searches.queries.{BoolQueryDefinition, QueryStringQueryDefinition}
import com.sksamuel.elastic4s.searches.sort.FieldSortDefinition
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.integration.ElasticClient
import no.gdl.bookapi.model.api.{Book, Error, GdlSearchException, LocalDateSerializer, ResultWindowTooLargeException, SearchResult}
import no.gdl.bookapi.model.domain.Paging
import no.gdl.bookapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.read

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


trait SearchService {
  this: ElasticClient with ConverterService with IndexBuilderService with IndexService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    implicit val formats = DefaultFormats + LocalDateSerializer

    def searchBook(query: Option[String], language: LanguageTag, paging: Paging): SearchResult =
      executeSearch(QueryBuilders.boolQuery(), query, language, paging)

    def executeSearch(queryBuilder: BoolQueryBuilder, query: Option[String], language: LanguageTag, paging: Paging): SearchResult = {

      val (startAt, numResults) = getStartAtAndNumResults(paging.page, paging.pageSize)

      val requestedResultWindow = paging.page * numResults
      if (requestedResultWindow > BookApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${BookApiProperties.ElasticSearchIndexMaxResultWindow}, user requested ${requestedResultWindow}")
        throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }

      val indexAndTypes = IndexAndTypes(BookApiProperties.searchIndex(language), Seq(BookApiProperties.SearchDocument))
      val searchResponse = esClient.execute(
        searchWithType(indexAndTypes).size(numResults).from(startAt)
          .bool(BoolQueryDefinition()
            .should(QueryStringQueryDefinition(query = query.getOrElse("*")).field("title").field("description")))
          .sortBy(FieldSortDefinition("id"))
      )

      searchResponse match {
        case Success(response) => SearchResult(response.result.totalHits, paging.page, numResults, converterService.toApiLanguage(language), getHits(response.result.hits, language))
        case Failure(failure) => errorHandler(Failure(failure))
      }
    }

    def getHits(hits: SearchHits, language: LanguageTag): Seq[Book] = {
      hits.hits.iterator.toSeq.map(hit => hitAsApiBook(hit.sourceAsString, language))
    }

    def hitAsApiBook(hit: String, language: LanguageTag): Book = {
      read[Book](hit)
    }

    private def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val startAt = (page-1) * pageSize
      (startAt, pageSize)
    }

    private def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: GdlSearchException) => {
          e.getFailure.status match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index ${BookApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index ${BookApiProperties.SearchIndex} not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getFailure.error.reason)
              throw new ElasticsearchException(s"Unable to execute search in ${BookApiProperties.SearchIndex}", e.getFailure.error.reason)
            }
          }

        }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        indexBuilderService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("Unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }


}
