/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

case class PdfCss(source: Option[String], pageOrientation: PageOrientation.Value, fontNames: Seq[String]) {

  def asString: String = {
    val pageSize = pageOrientation match {
      case PageOrientation.PORTRAIT => "a5"
      case PageOrientation.LANDSCAPE => "a5 landscape"
    }

    val imgMaxWidth = pageOrientation match {
      case PageOrientation.PORTRAIT => "400px"
      case PageOrientation.LANDSCAPE => "500px"
    }

    val imgPaddingBottom = pageOrientation match {
      case PageOrientation.PORTRAIT => "40px"
      case PageOrientation.LANDSCAPE => "20px"
    }

    val fontSize = pageOrientation match {
      case PageOrientation.PORTRAIT => "medium"
      case PageOrientation.LANDSCAPE => "small"
    }

    s"""
       |@page {
       |   size: $pageSize;
       |}
       |
       |div.page {
       |   page-break-after: always;
       |}
       |
       |body {
       |   margin: 0;
       |   text-align: center;
       |   font-family: '${fontNames.mkString("','")}', sans-serif;
       |   font-size: $fontSize;
       |}
       |
       |p {
       |   margin: 0;
       |}
       |
       |img {
       |   max-width: $imgMaxWidth;
       |   max-height: 400px;
       |   display: block;
       |   margin-left: auto;
       |   margin-right: auto;
       |   padding-bottom: $imgPaddingBottom;
       |   width: 75%;
       |   -fs-page-break-min-height: 5cm;
       |}
       |
       |.page_0 img.cover {
       |   width: 75%;
       |   height: 50%;
       |   display: block:
       |   margin-left: auto;
       |   margin-right: auto;
       |}
       |
       |.page_0 img.logo {
       |   width: 75px;
       |}
       |
       |.license {
       |   font-size: x-small;
       |}
       |
       |.license img {
       |   width: 100px;
       |}
    """.stripMargin
  }
}
