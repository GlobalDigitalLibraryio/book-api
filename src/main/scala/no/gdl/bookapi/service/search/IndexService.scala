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
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings.{MappingDefinition, TextFieldDefinition}
import com.sksamuel.elastic4s.{IndexAndType, RefreshPolicy}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.integration.ElasticClient
import no.gdl.bookapi.model.Language._
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.LocalDateSerializer
import no.gdl.bookapi.model.domain.Translation
import no.gdl.bookapi.repository.{BookRepository, TranslationRepository}
import no.gdl.bookapi.service.ConverterService
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization.write

import scala.collection.immutable
import scala.util.{Failure, Success, Try}

trait IndexService extends LazyLogging {
  this: ElasticClient with ConverterService with TranslationRepository with BookRepository =>
  val indexService: IndexService

  class IndexService {
    implicit val formats: Formats = DefaultFormats + LocalDateSerializer

    def indexDocument(translation: Translation): Try[Translation] = {
      indexExisting(BookApiProperties.searchIndex(translation.language)) match {
        case Success(false) =>
          val indexName = createSearchIndex(translation.language)
          updateAliasTarget(None, indexName.get, translation.language)
        case _ => // Does not matter
      }

      val availableLanguages: Seq[LanguageTag] = translationRepository.languagesFor(translation.bookId)
      val book: Option[domain.Book] = bookRepository.withId(translation.bookId)
      val source = write(converterService.toApiBook(Some(translation), availableLanguages, book))

      esClient.execute(
        indexInto(BookApiProperties.searchIndex(translation.language), BookApiProperties.SearchDocument)
          .id(translation.id.get.toString)
          .source(source)
      ) match {
        case Success(_) => Success(translation)
        case Failure(failure) => Failure(failure)
      }
    }

    def indexDocuments(translationList: List[Translation], indexName: String): Try[Int] = {
      val indexAndType = new IndexAndType(indexName, BookApiProperties.SearchDocument)
      val actions: immutable.Seq[IndexDefinition] = for {translation <- translationList
        availableLanguages: Seq[LanguageTag] = translationRepository.languagesFor(translation.bookId)
        book: Option[domain.Book] = bookRepository.withId(translation.bookId)
        source = write(converterService.toApiBook(Some(translation), availableLanguages, book))
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
        keywordField("externalId"),
        keywordField("uuid") index false,
        languageField("title", language, 2.0),
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

    private def languageField(fieldName: String, languageTag: LanguageTag, boost: Double = 1.0) = {
      val languageAnalyzer = findByLanguage(languageTag)
      val languageSupportedField = TextFieldDefinition(fieldName).fielddata(true) analyzer languageAnalyzer.analyzer boost boost
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

    def deleteSearchIndex(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
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