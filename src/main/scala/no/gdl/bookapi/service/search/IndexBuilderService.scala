/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.domain.{PublishingStatus, ReindexResult, Translation}
import no.gdl.bookapi.repository.TranslationRepository

import scala.util.{Failure, Success, Try}

trait IndexBuilderService {
  this: TranslationRepository with IndexService =>
  val indexBuilderService: IndexBuilderService

  class IndexBuilderService extends LazyLogging {

    def indexDocument(imported: Translation): Try[Translation] = {
      for {
        _ <- indexService.aliasTarget(imported.language).map {
          case Some(index) => Success(index)
          case None => indexService.createSearchIndex(imported.language).map(newIndex => indexService.updateAliasTarget(None, newIndex, imported.language))
        }
        imported <- indexService.indexDocument(imported)
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        var numIndexed = 0
        val start = System.currentTimeMillis()
        translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED).foreach(language => {
          indexDocumentsForLanguage(language) match {
            case Success(reindexResult) => numIndexed += reindexResult.totalIndexed
            case Failure(f) => Failure(f)
          }
        })
        Success(ReindexResult(numIndexed, System.currentTimeMillis() - start))
      }
    }

    def indexDocumentsForLanguage(languageTag: LanguageTag): Try[ReindexResult] = {
      val start = System.currentTimeMillis()
      indexService.createSearchIndex(languageTag).flatMap(f = indexName => {
        val operations = for {
          numIndexed <- sendToElastic(indexName, languageTag)
          aliasTarget <- indexService.aliasTarget(languageTag)
          _ <- indexService.updateAliasTarget(aliasTarget, indexName, languageTag)
          _ <- indexService.deleteSearchIndex(aliasTarget)
        } yield numIndexed

        operations match {
          case Failure(f) =>
            indexService.deleteSearchIndex(Some(indexName))
            Failure(f)
          case Success(totalIndexed) =>
            Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
        }
      })
    }

    def sendToElastic(indexName: String, languageTag: LanguageTag): Try[Int] = {
      var numIndexed = 0
      val numTranslations = translationRepository.numberOfTranslationsWithStatus(languageTag, PublishingStatus.PUBLISHED)
      val iterations = numTranslations / BookApiProperties.IndexBulkSize
      0 to iterations foreach(iter => {
        val numberInBulk = indexService.indexDocuments(translationRepository.translationsWithLanguageAndStatus(languageTag, PublishingStatus.PUBLISHED, BookApiProperties.IndexBulkSize, iter * BookApiProperties.IndexBulkSize), indexName)
        numberInBulk match {
          case Success(num) => numIndexed += num
          case Failure(f) => Failure(f)
        }
      })
      Success(numIndexed)
    }
  }

}
