/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

case class PdfCss (source: Option[String], pageOrientation: PageOrientation.Value, fontNames: Seq[String]) {

  def asString: String = {
    pageOrientation match {
      case PageOrientation.PORTRAIT => portraitCss
      case PageOrientation.LANDSCAPE => landscapeCss
    }
  }

  def landscapeCss: String = {
    s"""
      |@page {
      |   size: a5 landscape;
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
      |   font-size: small;
      |}
      |
      |p {
      |   margin: 0;
      |}
      |
      |img {
      |   max-width: 500px;
      |   max-height: 400px;
      |   display: block;
      |   margin-left: auto;
      |   margin-right: auto;
      |   padding-bottom: 20px;
      |   width: 75%;
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

  def portraitCss: String = {
    s"""
       |@page {
       |   size: a5;
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
       |   font-size: medium;
       |}
       |
       |p {
       |   margin: 0;
       |}
       |
       |img {
       |   max-width: 400px;
       |   max-height: 400px;
       |   display: block;
       |   margin-left: auto;
       |   margin-right: auto;
       |   padding-bottom: 40px;
       |   width: 75%;
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


