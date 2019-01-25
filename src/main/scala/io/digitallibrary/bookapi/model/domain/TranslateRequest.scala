package io.digitallibrary.bookapi.model.domain

case class TranslateRequest(bookId: Long,
                            fromLanguage: String,
                            toLanguage: String,
                            userId: Option[String])