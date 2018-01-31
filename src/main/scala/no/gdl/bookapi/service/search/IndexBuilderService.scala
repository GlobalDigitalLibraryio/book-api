/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain.{ReindexResult, Translation}
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
        var numIndexed = 0;
        val start = System.currentTimeMillis()
        getLanguages.map(languages => {
          languages.foreach(language => {
            indexService.createSearchIndex(language).flatMap(indexName => {
              val operations = for {
                numIndexed <- sendToElastic(indexName, language)
                aliasTarget <- indexService.aliasTarget(language)
                _ <- indexService.updateAliasTarget(aliasTarget, indexName, language)
                _ <- indexService.deleteSearchIndex(aliasTarget)
              } yield numIndexed

              operations match {
                case Failure(f) => {
                  indexService.deleteSearchIndex(Some(indexName))
                  Failure(f)
                }
                case Success(totalIndexed) => {
                  Success(numIndexed += totalIndexed)
                }
              }
            })
          })
        })
        Success(ReindexResult(numIndexed, System.currentTimeMillis() - start))
      }
    }

    def sendToElastic(indexName: String, language: LanguageTag): Try[Int] = {
      var numIndexed = 0
      val numberInBulk = indexService.indexDocuments(translationRepository.translationsWithLanguage(language), indexName)
      numberInBulk match {
        case Success(num) => numIndexed += num
        case Failure(f) => return Failure(f)
      }
      Success(numIndexed)
    }

    def getLanguages:Try[List[LanguageTag]] = {
      Try{
        val lang = translationRepository.allAvailableLanguages()
        logger.info(s"Number of languages: ${lang.size}")
        lang.toList
      }
    }
  }

}
