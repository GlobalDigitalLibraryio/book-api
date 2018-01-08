/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.model.api.{TranslateRequest, TranslateResponse}

import scala.util.{Success, Try}

trait TranslationService {
  val translationService: TranslationService

  class TranslationService extends LazyLogging {
    def addTranslation(translateRequest: TranslateRequest): Try[TranslateResponse] = {
      // TODO in #155: Add implementation of sending to crowdin
      Success(TranslateResponse(1, "https://www.digitallibrary.io"))
    }
  }
}
