/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.search

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.model.domain.{PublishingStatus, ReindexResult, Translation}
import io.digitallibrary.bookapi.repository.TranslationRepository

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
        val languages = unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)
        languages.foreach(language => {
          indexDocumentsForLanguage(language) match {
            case Success(reindexResult) => numIndexed += reindexResult.totalIndexed
            case Failure(f) => Failure(f)
          }
        })
        deleteOldIndexes(languages)
        Success(ReindexResult(numIndexed, System.currentTimeMillis() - start))
      }
    }

    def deleteOldIndexes(languages: Seq[LanguageTag]): Unit = {
      indexService.findAllIndexes() match {
        case Success(indexes) => indexes.foreach(index => {
          languages.map(language => BookApiProperties.searchIndex(language)).filter(alias => index.startsWith(s"${alias}_")) match {
            case _ :: Nil => // Do nothing, index OK
            case Nil => indexService.deleteSearchIndex(Some(index))
          }
        })
        case Failure(_) => logger.debug("Found no indexes")
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
      val numTranslations = unFlaggedTranslationsRepository.numberOfTranslationsWithStatus(languageTag, PublishingStatus.PUBLISHED)
      val pageSize = BookApiProperties.IndexBulkSize
      val pages = Math.round(Math.ceil(numTranslations.toFloat / pageSize)).toInt
      1 to pages foreach(page => {
        val toIndex = unFlaggedTranslationsRepository.withLanguageAndStatus(Some(languageTag), PublishingStatus.PUBLISHED, pageSize, page).results
        val numberInBulk = indexService.indexDocuments(toIndex.toList, indexName)
        numberInBulk match {
          case Success(num) => numIndexed += num
          case Failure(f) => Failure(f)
        }
      })
      Success(numIndexed)
    }
  }

}
