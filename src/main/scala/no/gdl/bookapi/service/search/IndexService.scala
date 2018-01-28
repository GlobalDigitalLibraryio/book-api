/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import java.text.SimpleDateFormat
import java.util.{Calendar, UUID}

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.{MappingContentBuilder, NestedFieldDefinition, ObjectFieldDefinition, TextFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.searchbox.core.{Bulk, Index}
import io.searchbox.indices.aliases._
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists, Stats}
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.integration.ElasticClient
import no.gdl.bookapi.model.domain.Translation
import no.gdl.bookapi.model.Language._
import no.gdl.bookapi.model.api.{GdlSearchException, LocalDateSerializer}
import no.gdl.bookapi.model.domain
import no.gdl.bookapi.repository.{BookRepository, TranslationRepository}
import no.gdl.bookapi.service.ConverterService
import org.elasticsearch.ElasticsearchException
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
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
      val indexRequest = new Index.Builder(source).index(BookApiProperties.searchIndex(translation.language)).`type`(BookApiProperties.SearchDocument).id(translation.id.get.toString).build

      jestClient.execute(indexRequest).map(_ => translation)
    }

    def indexDocuments(translationList: List[Translation], indexName: String): Try[Int] = {
      val bulkBuilder = new Bulk.Builder()
      translationList.foreach(translation => {
        val availableLanguages: Seq[LanguageTag] = translationRepository.languagesFor(translation.bookId)
        val book: Option[domain.Book] = bookRepository.withId(translation.bookId)
        val source = write(converterService.toApiBook(Some(translation), availableLanguages, book))
        bulkBuilder.addAction(new Index.Builder(source).index(indexName).`type`(BookApiProperties.SearchDocument).id(translation.id.get.toString).build)
      })

      val response = jestClient.execute(bulkBuilder.build())
      response.map(_ => {
        logger.info(s"Indexed ${translationList.size} documents")
        translationList.size
      })
    }

    def createIndex(language: LanguageTag): Try[String] = {
      logger.info(s"Create index for language: ${language.language.id}")
      createIndexWithName(BookApiProperties.searchIndex(language) + "_" + getTimestamp + "_" + UUID.randomUUID().toString, language)
    }

    def createIndexWithName(indexName: String, language: LanguageTag): Try[String] = {
      if (indexExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val createIndexResponse = jestClient.execute(
          new CreateIndex.Builder(indexName)
            .settings(createSettings())
            .build())
        createIndexResponse.map(_ => createMapping(indexName, language)).map(_ => indexName)
      }
    }

    def createSettings(): String = {
      s"""{"index":{"max_result_window":${BookApiProperties.ElasticSearchIndexMaxResultWindow}}}""".stripMargin
    }

    def createMapping(indexName: String, language: LanguageTag): Try[String] = {
      val mapping = buildMapping(language)
      logger.info(mapping)
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, BookApiProperties.SearchDocument, mapping).build())
      mappingResponse.map(_ => indexName)
    }

    def buildMapping(language: LanguageTag): String = {
      MappingContentBuilder.buildWithName(mapping(BookApiProperties.SearchDocument).fields(
        intField("id"),
        intField("revision"),
        intField("externalId"),
        keywordField("uuid") index "false",
        languageField("title", language),
        languageField("description", language),
        objectField("translatedFrom").as(
          keywordField("code"),
          keywordField("name")
        ),
        objectField("language").as(
          keywordField("code"),
          keywordField("name")
        ),
        nestedField("availableLanguages").as(
          keywordField("code"),
          keywordField("name")
        ),
        objectField("licence").as(
          intField("id"),
          intField("revision"),
          keywordField("name"),
          keywordField("description"),
          keywordField("url")
        ),
        objectField("publisher").as(
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
        nestedField("categories").as(
          textField("category")
        ),
        objectField("coverPhoto").as(
          textField("large"),
          textField("small")
        ),
        objectField("downloads").as(
          textField("epub"),
          textField("pdf")
        ),
        //nestedField("tags"),
        nestedField("contributors").as(
          intField("id"),
          intField("revision"),
          keywordField("type"),
          textField("name")
        ),
        nestedField("chapters").as(
          intField("id"),
          intField("seqNo"),
          languageField("title", language),
          textField("url")
        ),
        booleanField("supportsTranslation")
      ), BookApiProperties.SearchDocument).string()
    }

    private def languageField(fieldName: String, language: LanguageTag) = {
      val languageAnalyzer = languageAnalyzers.find(p => p.lang.equals(language.language.id)).get
      val languageSupportedField = new TextFieldDefinition(fieldName).fielddata(true) analyzer languageAnalyzer.analyzer
      languageSupportedField
    }

    def findAllIndexes(): Try[Seq[String]] = {
      jestClient.execute(new Stats.Builder().build())
        .map(r => r.getJsonObject.get("indices").getAsJsonObject.entrySet().asScala.map(_.getKey).toSeq)
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String, language: LanguageTag): Try[Any] = {
      if (!indexExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, BookApiProperties.searchIndex(language)).build()
        val modifyAliasRequest = oldIndexName match {
          case None => new ModifyAliases.Builder(addAliasDefinition).build()
          case Some(oldIndex) => {
            new ModifyAliases.Builder(
              new RemoveAliasMapping.Builder(oldIndex, BookApiProperties.searchIndex(language)).build()
            ).addAlias(addAliasDefinition).build()
          }
        }

        jestClient.execute(modifyAliasRequest)
      }
    }

    def deleteIndex(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) => {
          if (!indexExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            jestClient.execute(new DeleteIndex.Builder(indexName).build())
          }
        }
      }
    }

    def aliasTarget(language: LanguageTag): Try[Option[String]] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${BookApiProperties.searchIndex(language)}").build()
      jestClient.execute(getAliasRequest) match {
        case Success(result) => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Success(Some(aliasIterator.next().getKey))
            case false => Success(None)
          }
        }
        case Failure(_: GdlSearchException) => Success(None)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def indexExists(indexName: String): Try[Boolean] = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()) match {
        case Success(_) => Success(true)
        case Failure(_: ElasticsearchException) => Success(false)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
