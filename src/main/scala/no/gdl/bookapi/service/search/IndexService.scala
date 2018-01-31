/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import java.text.SimpleDateFormat
import java.util.{Calendar, UUID}

import com.sksamuel.elastic4s.alias.{AddAliasActionDefinition, AliasActionDefinition, RemoveAliasActionDefinition}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings.{MappingDefinition, TextFieldDefinition}
import com.sksamuel.elastic4s.{IndexAndType, RefreshPolicy}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.integration.ElasticClient
import no.gdl.bookapi.model.Language._
import no.gdl.bookapi.model.api.{GdlSearchException, LocalDateSerializer}
import no.gdl.bookapi.model.domain
import no.gdl.bookapi.model.domain.Translation
import no.gdl.bookapi.repository.{BookRepository, TranslationRepository}
import no.gdl.bookapi.service.ConverterService
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient with ConverterService with TranslationRepository with BookRepository =>
  val indexService: IndexService

  class IndexService extends LazyLogging {
    implicit val formats = DefaultFormats + LocalDateSerializer

    def indexDocument(translation: Translation): Try[Translation] = {
      val availableLanguages: Seq[LanguageTag] = translationRepository.languagesFor(translation.bookId)
      val book: Option[domain.Book] = bookRepository.withId(translation.bookId)
      val source = write(converterService.toApiBook(Some(translation), availableLanguages, book))

      val indexResponse = esClient.execute(
        indexInto(BookApiProperties.searchIndex(translation.language), BookApiProperties.SearchDocument)
          .id(translation.id.toString)
          .source(source)
      ).await

      indexResponse match {
        case Left(failure) => Failure(new GdlSearchException(failure))
        case Right(_) => Success(translation)
      }
    }

    def indexDocuments(translationList: List[Translation], indexName: String): Try[Int] = {
      var actions = List[IndexDefinition]()
      translationList.foreach(f = translation => {
        val availableLanguages: Seq[LanguageTag] = translationRepository.languagesFor(translation.bookId)
        val book: Option[domain.Book] = bookRepository.withId(translation.bookId)
        val source = write(converterService.toApiBook(Some(translation), availableLanguages, book))
        val indexAndType = new IndexAndType(indexName, BookApiProperties.SearchDocument)
        actions = actions :+ IndexDefinition(indexAndType, id = Some(translation.id.toString), source = Some(source))
      })

      // TODO Limit bulk size
      val bulkResponse = esClient.execute(
        bulk(actions).refresh(RefreshPolicy.WAIT_UNTIL)
      ).await

      bulkResponse match {
        case Left(failure) => Failure(new GdlSearchException(failure))
        case Right(_) => {
          logger.info(s"Indexed ${translationList.size} documents")
          Success(translationList.size)
        }
      }
    }

    def createSearchIndex(language: LanguageTag): Try[String] = {
      logger.info(s"Create index for language: ${language.language.id}")
      createSearchIndexWithName(BookApiProperties.searchIndex(language) + "_" + getTimestamp + "_" + UUID.randomUUID().toString, language)
    }

    def createSearchIndexWithName(indexName: String, language: LanguageTag): Try[String] = {
      if (indexExisting(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val createIndexResponse = esClient.execute(
          createIndex(indexName)
            .settings(settings())
            .mappings(mappings(language))
        ).await

        createIndexResponse match {
          case Left(failure) => {
            logger.error(failure.error.reason)
            Failure(new GdlSearchException(failure))
          }
          case Right(_) => Success(indexName)
        }
      }
    }

    def settings(): Map[String, Any] = {
      Map(
        "max_result_window" -> BookApiProperties.ElasticSearchIndexMaxResultWindow,
        "analysis" -> Map(
          "analyzer" -> Map(
            "babel" -> Map(
              "type" -> "custom",
              "tokenizer" -> "icu_tokenizer",
              "char_filter" -> Seq("icu_normalizer"),
              "filter" -> Seq("icu_folding","icu_collation")
            )
          )
        )
      )
    }

    def mappings(language: LanguageTag): List[MappingDefinition] = {
      List(mapping(BookApiProperties.SearchDocument).fields(
        intField("id"),
        intField("revision"),
        intField("externalId"),
        keywordField("uuid") index false,
        languageField("title", language),
        languageField("description", language),
        objectField("translatedFrom").fields(
          keywordField("code"),
          keywordField("name")
        ),
        objectField("language").fields(
          keywordField("code"),
          keywordField("name")
        ),
        nestedField("availableLanguages").fields(
          keywordField("code"),
          keywordField("name")
        ),
        objectField("licence").fields(
          intField("id"),
          intField("revision"),
          keywordField("name"),
          keywordField("description"),
          keywordField("url")
        ),
        objectField("publisher").fields(
          intField("id"),
          intField("revision"),
          keywordField("name")
        ),
        keywordField("readingLevel"),
        keywordField("typicalAgeRange"),
        keywordField("educationalUse"),
        keywordField("educationalRole"),
        keywordField("timeRequired"),
        dateField("datePublished"),
        dateField("dateCreated"),
        dateField("dateArrived"),
        nestedField("categories").fields(
          textField("category")
        ),
        objectField("coverPhoto").fields(
          textField("large"),
          textField("small")
        ),
        objectField("downloads").fields(
          textField("epub"),
          textField("pdf")
        ),
        //nestedField("tags"),
        nestedField("contributors").fields(
          intField("id"),
          intField("revision"),
          keywordField("type"),
          textField("name")
        ),
        nestedField("chapters").fields(
          intField("id"),
          intField("seqNo"),
          languageField("title", language),
          textField("url")
        ),
        booleanField("supportsTranslation")
      ))
    }

    private def languageField(fieldName: String, language: LanguageTag) = {
      val languageAnalyzer = findByLanguage(Some(language.language.id))
      val languageSupportedField = TextFieldDefinition(fieldName).fielddata(true) analyzer languageAnalyzer.get.analyzer
      languageSupportedField
    }

    def findAllIndexes(): Try[Seq[String]] = {
      val indexesResponse = esClient.execute(
        getAliases()
      ).await

      indexesResponse match {
        case Left(failure) => Failure(new GdlSearchException(failure))
        case Right(response) => Success(response.result.mappings.keys.toSeq.map(_.name))
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String, language: LanguageTag): Try[Any] = {
      if (!indexExisting(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        var actions = List[AliasActionDefinition](AddAliasActionDefinition(BookApiProperties.searchIndex(language), newIndexName))
        oldIndexName match {
          case None => // Do nothing
          case Some(oldIndex) => {
            actions = actions :+ RemoveAliasActionDefinition(BookApiProperties.searchIndex(language), oldIndex)
          }
        }
        val aliasResponse = esClient.execute(
          aliases(actions)
        ).await

        aliasResponse match {
          case Left(failure) => Failure(new GdlSearchException(failure))
          case Right(_) => Success()
        }
      }
    }

    def deleteSearchIndex(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) => {
          if (!indexExisting(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            val deleteResponse = esClient.execute(
              deleteIndex(indexName)
            ).await
            deleteResponse match {
              case Left(failure) => Failure(new GdlSearchException(failure))
              case Right(_) => Success()
            }
          }
        }
      }
    }

    def aliasTarget(language: LanguageTag): Try[Option[String]] = {

      val aliasesResponse = esClient.execute(
        getAliases(BookApiProperties.searchIndex(language),Nil)
      ).await

      aliasesResponse match {
        case Left(failure) => Failure(new GdlSearchException(failure))
        case Right(response) => {
          val iterator = response.result.mappings.iterator
          iterator.hasNext match {
            case false => Success(None)
            case true => Success(Some(iterator.next()._1.name))
          }
        }
      }
    }

    def indexExisting(indexName: String): Try[Boolean] = {
      esClient.execute(indexExists(indexName)).await match {
        case Left(failure) => {
          logger.error(failure.error.reason)
          Success(false)
        }
        case Right(response) => Success(response.result.isExists)
      }
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
