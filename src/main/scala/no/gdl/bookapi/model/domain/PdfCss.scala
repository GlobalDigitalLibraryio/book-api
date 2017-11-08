/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

case class PdfCss (publisher: Option[String], fontNames: Seq[String]) {

  val DefaultPageSize = "a4"
  val landscapePublishers = Map(
    "pratham books" -> "a5 landscape"
  )

  def asString: String = {
    s"""
      |div.page {
      | page-break-after: always;
      |}
      |
      |body {
      |    margin: 0;
      |    text-align: center;
      |    font-family: '${fontNames.mkString("','")}', sans-serif;
      |    font-size: small;
      |}
      |
      |p {
      |   margin: 0;
      |}
      |
      |img {
      |    max-width: 700px;
      |    max-height: 300px;
      |}
      |@page {
      | size: ${getPageSize(publisher)};
      |}
    """.stripMargin
  }

  private def getPageSize(publisherOpt: Option[String]) = {
    publisherOpt.flatMap(pub => landscapePublishers.get(pub.toLowerCase)).getOrElse(DefaultPageSize)
  }
}


