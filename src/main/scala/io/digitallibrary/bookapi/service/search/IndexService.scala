/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.search

import java.text.SimpleDateFormat
import java.util.{Calendar, UUID}

import com.sksamuel.elastic4s.alias.{AddAliasActionDefinition, AliasActionDefinition, RemoveAliasActionDefinition}
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings.{KeywordFieldDefinition, MappingDefinition, TextFieldDefinition}
import com.sksamuel.elastic4s.{IndexAndType, RefreshPolicy}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.ElasticClient
import io.digitallibrary.bookapi.model.Language._
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api.LocalDateSerializer
import io.digitallibrary.bookapi.model.domain.{PublishingStatus, Translation}
import io.digitallibrary.bookapi.repository.{BookRepository, TranslationRepository}
import io.digitallibrary.bookapi.service.ConverterService
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization.write

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

trait IndexService extends LazyLogging {
  this: ElasticClient with ConverterService with TranslationRepository with BookRepository =>
  val indexService: IndexService

  class IndexService {
    implicit val formats: Formats = DefaultFormats + LocalDateSerializer

    def updateOrRemoveDocument(translation: Translation): Try[Translation] = {
      translation.publishingStatus match {
        case PublishingStatus.PUBLISHED => indexDocument(translation)
        case PublishingStatus.FLAGGED => removeDocument(translation)
        case PublishingStatus.UNLISTED => removeDocument(translation)
      }
    }

    def indexDocument(translation: Translation): Try[Translation] = {
      indexExisting(BookApiProperties.searchIndex(translation.language)) match {
        case Success(false) =>
          val indexName = createSearchIndex(translation.language)
          updateAliasTarget(None, indexName.get, translation.language)
        case _ => // Does not matter
      }

      val book: Option[domain.Book] = bookRepository.withId(translation.bookId)
      val source = write(converterService.toApiBookHit(Some(translation), book))

      esClient.execute(
        indexInto(BookApiProperties.searchIndex(translation.language), BookApiProperties.SearchDocument)
          .id(translation.id.get.toString)
          .source(source)
      ) match {
        case Success(_) => Success(translation)
        case Failure(failure) => Failure(failure)
      }
    }

    def removeDocument(translation: Translation): Try[Translation] = {
      indexExisting(BookApiProperties.searchIndex(translation.language)) match {
        case Success(false) =>
          val indexName = createSearchIndex(translation.language)
          updateAliasTarget(None, indexName.get, translation.language)
        case _ => // Does not matter
      }

      esClient.execute(
        deleteById(BookApiProperties.searchIndex(translation.language), BookApiProperties.SearchDocument, translation.id.get.toString)
      ) match {
        case Success(_) => Success(translation)
        case Failure(failure) => Failure(failure)
      }
    }

    def indexDocuments(translationList: List[Translation], indexName: String): Try[Int] = {
      val indexAndType = new IndexAndType(indexName, BookApiProperties.SearchDocument)
      val actions: immutable.Seq[IndexDefinition] = for {
        translation <- translationList
        book: Option[domain.Book] = bookRepository.withId(translation.bookId)
        source = write(converterService.toApiBookHitV2(Some(translation), book))
      } yield IndexDefinition(indexAndType, id = Some(translation.id.get.toString), source = Some(source))

      esClient.execute(
        bulk(actions).refresh(RefreshPolicy.WAIT_UNTIL)
      ) match {
        case Failure(failure) => Failure(failure)
        case Success(_) =>
          logger.info(s"Indexed ${translationList.size} documents")
          Success(translationList.size)
      }
    }

    def createSearchIndex(languageTag: LanguageTag): Try[String] = {
      logger.info(s"Create index for language: ${languageTag.toString}")
      createSearchIndexWithName(BookApiProperties.searchIndex(languageTag) + "_" + getTimestamp + "_" + UUID.randomUUID().toString, languageTag)
    }

    def createSearchIndexWithName(indexName: String, languageTag: LanguageTag): Try[String] = {
      if (indexExisting(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        esClient.execute(
          createIndex(indexName)
            .indexSetting("max_result_window",BookApiProperties.ElasticSearchIndexMaxResultWindow)
            .mappings(mappings(languageTag))
            .analysis(analysis())
          ) match {
          case Failure(failure) =>
            logger.error(failure.getMessage)
            Failure(failure)
          case Success(_) => Success(indexName)
        }
      }
    }

    private def analysis(): Iterable[AnalyzerDefinition] = {
      List(CustomAnalyzerDefinition(
        BabelAnalyzer.name,
        IcuTokenizer,
        IcuNormalizer,
        IcuFolding,
        IcuCollation
      ))
    }

    private def mappings(language: LanguageTag): List[MappingDefinition] = {
      List(mapping(BookApiProperties.SearchDocument) as (
        intField("id"),
        intField("revision"),
        languageField("title", language, 1.4, true),
        languageField("description", language),
        objectField("language").fields(
          keywordField("code"),
          keywordField("name")
        ),
        keywordField("readingLevel"),
        dateField("dateArrived"),
        keywordField("source"),
        objectField("coverImage").fields(
          textField("id")
        )
      ))
    }

    private def languageField(fieldName: String, languageTag: LanguageTag, boost: Double = 1.0, keepRaw: Boolean = false) = {
      val languageAnalyzer = findByLanguage(languageTag)
      val languageSupportedField = keepRaw match {
        case true => TextFieldDefinition(fieldName).fielddata(true).fields(KeywordFieldDefinition("sort")) analyzer languageAnalyzer.analyzer boost boost
        case false => TextFieldDefinition(fieldName).fielddata(true) analyzer languageAnalyzer.analyzer boost boost
      }
      languageSupportedField
    }

    def findAllIndexes(): Try[Seq[String]] = {
      esClient.execute(
        getAliases()
      ) match {
        case Failure(failure) => Failure(failure)
        case Success(response) => Success(response.result.mappings.keys.toSeq.map(_.name).filter(name => name.startsWith(BookApiProperties.SearchIndex)))
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String, languageTag: LanguageTag): Try[Any] = {
      if (!indexExisting(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        var actions = List[AliasActionDefinition](AddAliasActionDefinition(BookApiProperties.searchIndex(languageTag), newIndexName))
        oldIndexName match {
          case None => // Do nothing
          case Some(oldIndex) =>
            actions = actions :+ RemoveAliasActionDefinition(BookApiProperties.searchIndex(languageTag), oldIndex)
        }
        esClient.execute(
          aliases(actions)
        ) match {
          case Failure(failure) => Failure(failure)
          case Success(_) => Success()
        }
      }
    }

    def deleteSearchIndex(optIndexName: Option[String]): Try[Unit] = {
      optIndexName match {
        case None => Success()
        case Some(indexName) =>
          if (!indexExisting(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            esClient.execute(
              deleteIndex(indexName)
            ) match {
              case Failure(failure) => Failure(failure)
              case Success(_) => Success()
            }
          }
      }
    }

    def aliasTarget(languageTag: LanguageTag): Try[Option[String]] = {
      esClient.execute(
        getAliases(BookApiProperties.searchIndex(languageTag),Nil)
      ) match {
        case Failure(failure) => Failure(failure)
        case Success(response) =>
          response.result.mappings.headOption match {
            case Some((index, _)) => Success(Some(index.name))
            case None => Success(None)
          }
      }
    }

    def indexExisting(indexName: String): Try[Boolean] = {
      esClient.execute(indexExists(indexName)) match {
        case Failure(failure) =>
          logger.error(failure.getMessage)
          Success(false)
        case Success(response) => Success(response.result.isExists)
      }
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }
}