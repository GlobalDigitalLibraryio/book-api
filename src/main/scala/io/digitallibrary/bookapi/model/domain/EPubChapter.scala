/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

case class EPubChapter(seqNo: Int, content: String, ePubCss: EPubCss, mimeType: String = "application/xhtml+xml") {
  def href: String = {
    s"chapter-$seqNo.xhtml"
  }

  def title: String = {
    s"Chapter $seqNo"
  }

  def asBytes: Array[Byte] = {
    s"""
       |<html xmlns="http://www.w3.org/1999/xhtml">
       |<head>
       |    <title>$title</title>
       |    <link href="${ePubCss.href}" rel="stylesheet" type="${ePubCss.mimeType}"/>
       |</head>
       |<body>$content</body>
       |</html>
    """.stripMargin.getBytes
  }
}