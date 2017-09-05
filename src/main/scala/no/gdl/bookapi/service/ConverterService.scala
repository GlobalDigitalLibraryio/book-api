/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.network.ApplicationUrl
import no.gdl.bookapi.{BookApiProperties, model}
import no.gdl.bookapi.model.api._


trait ConverterService {
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    val DefaultLicense = License("cc-by-4.0", Some("Creative Commons Attribution 4.0 International"), Some("https://creativecommons.org/licenses/by/4.0/"))
    val licenses = Map(
      "cc-by-4.0" -> DefaultLicense,
      "cc-by-sa-4.0" -> License("cc-by-sa-4.0", Some("Creative Commons Attribution-ShareAlike 4.0 International"), Some("https://creativecommons.org/licenses/by-sa/4.0/")),
      "cc-by-nc-4.0" -> License("cc-by-nc-4.0", Some("Creative Commons Attribution-NonCommercial 4.0 International"), Some("https://creativecommons.org/licenses/by-nc/4.0/")),
      "cc-by-nd-4.0" -> License("cc-by-nd-4.0", Some("Creative Commons Attribution-NoDerivatives 4.0 International"), Some("https://creativecommons.org/licenses/by-nd/4.0/")),
      "cc-by-nc-sa-4.0" -> License("cc-by-nc-sa-4.0", Some("Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International"), Some("https://creativecommons.org/licenses/by-nd/4.0/"))
    )

    def toDomainBookInLanguage(bookInLanguage: NewBookInLanguage): model.domain.BookInLanguage = {
      model.domain.BookInLanguage(
        None,
        None,
        None,
        bookInLanguage.externalId,
        bookInLanguage.title,
        bookInLanguage.description,
        bookInLanguage.language,
        model.domain.CoverPhoto(bookInLanguage.coverPhoto.large, bookInLanguage.coverPhoto.small),
        model.domain.Downloads(bookInLanguage.downloads.epub),
        bookInLanguage.dateCreated,
        bookInLanguage.datePublished,
        bookInLanguage.tags,
        bookInLanguage.authors
      )
    }

    def toDomainBook(newBook: NewBook): model.domain.Book = {
      val bookInLanguage = model.domain.BookInLanguage(
        None,
        None,
        None,
        newBook.externalId,
        newBook.title,
        newBook.description,
        newBook.language,
        model.domain.CoverPhoto(newBook.coverPhoto.large, newBook.coverPhoto.small),
        model.domain.Downloads(newBook.downloads.epub),
        newBook.dateCreated,
        newBook.datePublished,
        newBook.tags,
        newBook.authors
      )

      model.domain.Book(
        None,
        None,
        newBook.externalId,
        newBook.title,
        newBook.description,
        newBook.language,
        licenses.getOrElse(newBook.license, DefaultLicense).license,
        newBook.publisher,
        newBook.readingLevel,
        newBook.typicalAgeRange,
        newBook.educationalUse,
        newBook.educationalRole,
        newBook.timeRequired,
        newBook.categories,
        Seq(bookInLanguage))
    }

    def toApiBook(book: model.domain.Book, language: String): Option[Book] = {
      book.bookInLanguage.foreach(b => print(s"${b.externalId} - ${b.title}"))
      book.bookInLanguage.find(a => a.language == language).map(rmInLanguage => {
        Book(
          book.id.get,
          book.revision.get,
          rmInLanguage.externalId,
          "UUID", // TODO: #17 - UUID from DB
          rmInLanguage.title,
          rmInLanguage.description,
          language,
          book.bookInLanguage.map(_.language),
          licenses.getOrElse(book.license, DefaultLicense),
          Publisher(1, book.publisher), // TODO: #17 - Publisher from DB
          book.readingLevel,
          book.typicalAgeRange,
          book.educationalUse,
          book.educationalRole,
          book.timeRequired,
          rmInLanguage.datePublished,
          rmInLanguage.dateCreated,
          book.categories.map(x => Category(1, x)), // TODO: #17 - Category from DB
          toApiCoverPhoto(rmInLanguage.coverPhoto),
          toApiDownloads(rmInLanguage.downloads),
          rmInLanguage.tags,
          rmInLanguage.authors.map(x => Contributor(1, "author", x)), // TODO: #17 - Contributor from DB
          Seq())
      })
    }

    def toApiDownloads(downloads: model.domain.Downloads): Downloads = {
      Downloads(
        epub = s"${ApplicationUrl.getHost}${BookApiProperties.EpubPath}/${downloads.epub}", // TODO: #17 - Download EPub
        pdf = s"${ApplicationUrl.getHost}${BookApiProperties.EpubPath}/${downloads.epub}") // TODO: #17 - Download PDF
    }

    def toApiCoverPhoto(coverPhoto: model.domain.CoverPhoto): CoverPhoto = {
      val small = s"${ApplicationUrl.getHost}${BookApiProperties.ImagePath}/${coverPhoto.small}?width=${BookApiProperties.CoverPhotoTumbSize}"
      val large = s"${ApplicationUrl.getHost}${BookApiProperties.ImagePath}/${coverPhoto.large}"

      CoverPhoto(large, small)
    }
  }
}
