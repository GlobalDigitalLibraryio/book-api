package no.gdl.bookapi.service.search

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.integration.ElasticClient
import no.gdl.bookapi.model.api.{Book, Error, GdlSearchException, LocalDateSerializer, ResultWindowTooLargeException, SearchResult}
import no.gdl.bookapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilders
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

    def search(query: Option[String], language: LanguageTag, page: Int, pageSize: Int): SearchResult =
      executeSearch(QueryBuilders.boolQuery(), query, language, page, pageSize)

    def executeSearch(queryBuilder: BoolQueryBuilder, query: Option[String], language: LanguageTag, page: Int, pageSize: Int): SearchResult = {

      /*val licensedFiltered = license match {
        case None => queryBuilder.filter(noCopyright)
        case Some(lic) => queryBuilder.filter(QueryBuilders.termQuery("license", lic))
      }

      val sizeFiltered = minimumSize match {
        case None => licensedFiltered
        case Some(size) => licensedFiltered.filter(QueryBuilders.rangeQuery("imageSize").gte(minimumSize.get))
      }

      val languageFiltered = language match {
        case None => sizeFiltered
        case Some(lang) => sizeFiltered.filter(QueryBuilders.nestedQuery("titles", QueryBuilders.existsQuery(s"titles.$lang"), ScoreMode.Avg))
      }*/

      val queryString = queryBuilder.should(QueryBuilders.queryStringQuery(query.getOrElse("*")))

      //val languageFiltered = queryString.filter(QueryBuilders.termQuery("language.code", language.language.id))

      val search = new SearchSourceBuilder().query(queryString).sort(SortBuilders.fieldSort("id"))

      val (startAt, numResults) = getStartAtAndNumResults(Some(page), Some(pageSize))
      val request = new Search.Builder(search.toString).addIndex(BookApiProperties.searchIndex(language)).setParameter(Parameters.SIZE, numResults).setParameter("from", startAt).build()

      val requestedResultWindow = page * numResults
      if (requestedResultWindow > BookApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${BookApiProperties.ElasticSearchIndexMaxResultWindow}, user requested ${requestedResultWindow}")
        throw new ResultWindowTooLargeException(Error.WindowTooLargeError.description)
      }

      jestClient.execute(request) match {
        case Success(response) => SearchResult(response.getTotal.toLong, page, numResults, converterService.toApiLanguage(language), getHits(response, language))
        case Failure(f) => errorHandler(Failure(f))
      }
    }

    def getHits(response: JestSearchResult, language: LanguageTag): Seq[Book] = {
      var resultList = Seq[Book]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while(iterator.hasNext) {
            resultList = resultList :+ hitAsApiBook(iterator.next().asInstanceOf[JsonObject].get("_source").toString, language)
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsApiBook(hit: String, language: LanguageTag): Book = {
      read[Book](hit)
    }

    def countDocuments(): Int = {
      val ret = jestClient.execute(
        new Count.Builder().addIndex(BookApiProperties.SearchIndex).build()
      ).map(result => result.getCount.toInt)
      ret.getOrElse(0)
    }

    private def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(BookApiProperties.MaxPageSize) else BookApiProperties.DefaultPageSize
        case None => BookApiProperties.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None => 0
      }

      (startAt, numResults)
    }

    private def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: GdlSearchException) => {
          e.getResponse.getResponseCode match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index ${BookApiProperties.SearchIndex} not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index ${BookApiProperties.SearchIndex} not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getResponse.getErrorMessage)
              throw new ElasticsearchException(s"Unable to execute search in ${BookApiProperties.SearchIndex}", e.getResponse.getErrorMessage)
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
