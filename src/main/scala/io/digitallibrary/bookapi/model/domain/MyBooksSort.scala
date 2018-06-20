/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

object MyBooksSort  extends Enumeration {
  val ByIdDesc = Value("-id")
  val ByIdAsc = Value("id")
  val ByTitleDesc = Value("-title")
  val ByTitleAsc = Value("title")

  def valueOf(s:String): Option[MyBooksSort.Value] = {
    MyBooksSort.values.find(_.toString == s)
  }

  def valueOf(s:Option[String]): Option[MyBooksSort.Value] = {
    s match {
      case None => None
      case Some(s) => valueOf(s)
    }
  }
}