/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

case class PdfCss (source: Option[String], fontNames: Seq[String]) {

  val DefaultPageSize = "a4"
  val landscapeSources = Map(
    "storyweaver" -> "a5 landscape",
    "taf" -> "a5 landscape",
    "bookdash" -> "a5 landscape"
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
      | size: ${getPageSize(source)};
      |}
    """.stripMargin
  }

  private def getPageSize(sourceOpt: Option[String]) = {
    sourceOpt.flatMap(src => landscapeSources.get(src.toLowerCase)).getOrElse(DefaultPageSize)
  }
}


