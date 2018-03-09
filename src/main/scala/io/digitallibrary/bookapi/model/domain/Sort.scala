/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain


object Sort  extends Enumeration {
  val ByIdDesc = Value("-id")
  val ByIdAsc = Value("id")
  val ByTitleDesc = Value("-title")
  val ByTitleAsc = Value("title")
  val ByArrivalDateDesc = Value("-arrivaldate")
  val ByArrivalDateAsc = Value("arrivaldate")
  val ByRelevance = Value("score")

  def valueOf(s:String): Option[Sort.Value] = {
    Sort.values.find(_.toString == s)
  }

  def valueOf(s:Option[String]): Option[Sort.Value] = {
    s match {
      case None => None
      case Some(s) => valueOf(s)
    }
  }
}