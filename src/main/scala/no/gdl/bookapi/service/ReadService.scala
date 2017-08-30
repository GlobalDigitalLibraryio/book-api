/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service


import no.gdl.bookapi.model.domain.{Book, BookInLanguage}
import no.gdl.bookapi.repository.BooksRepository

trait ReadService {
  this: BooksRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def withTitle(title: String): Option[Book] = {
      booksRepository.withTitle(title)
    }

    def withId(id: Long): Option[Book] = {
      booksRepository.withId(id)
    }

    def withExternalId(externalId: Option[String]): Option[Book] = {
      externalId.flatMap(booksRepository.withExternalId)
    }

    def bookInLanguageWithExternalId(externalId: Option[String]): Option[BookInLanguage] = {
      externalId.flatMap(booksRepository.bookInLanguageWithExternalId)
    }

    def all(language: String): Seq[Book] = {
      booksRepository.all()
    }
  }
}
