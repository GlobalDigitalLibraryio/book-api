/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import java.time.LocalDate
import java.util.UUID

import no.gdl.bookapi.model.api._

object TestData {
  val DefaultUuid = UUID.randomUUID().toString

  val english = Language("eng", "English")
  val norwegian_bokmal = Language("nob", "Bokmål, Norwegian")

  val DefaultLicense = License(1, 1, "lisens", Some("beskrivelse"), Some("url"))
  val DefaultPublisher = Publisher(1, 1, "Some Publisher")

  val Level1 = "1"
  val ageRangeDefault = "5-10"

  val today = LocalDate.now()
  val yesterday = today.minusDays(1)

  val category1 = Category(1, 1, "category1")
  val category2 = Category(2, 1, "category2")

  val epub = "url-to-epub"
  val pdf = "url-to-pdf"

  val DefaultContributor = Contributor(1, 1, "type", "contributorname")

  val ChapterSummary1 = ChapterSummary(1, 1, Some("Title"), "some-url")
  val Chapter1 = Chapter(1, 1, 1, Some("Title"), "Content")

  val DefaultBook = Book(
    1, 1, Some("external-1"), DefaultUuid, "Title", "Description", english, Seq(english, norwegian_bokmal), DefaultLicense, DefaultPublisher,
    Some(Level1), Some(ageRangeDefault), None, None, None, Some(today), Some(yesterday), Seq(category1, category2), None, Downloads(epub, pdf), Seq(), Seq(DefaultContributor), Seq(ChapterSummary1))

}
