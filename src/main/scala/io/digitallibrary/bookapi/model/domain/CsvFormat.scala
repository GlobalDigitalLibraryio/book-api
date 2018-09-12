package io.digitallibrary.bookapi.model.domain

import scala.util.{Failure, Success, Try}

object CsvFormat extends Enumeration {
  val QualityAssurance, GooglePlay = Value

  def valueOf(s: String): Try[CsvFormat.Value] = {
    CsvFormat.values.find(_.toString.equalsIgnoreCase(s)) match {
      case Some(x) => Success(x)
      case None => Failure(new RuntimeException(s"Unknown CsvFormat $s."))
    }
  }

  def valueOfOrDefault(s: String): CsvFormat.Value = {
    valueOf(s).getOrElse(CsvFormat.QualityAssurance)
  }

  def valueOfOrDefault(s: Option[String]): CsvFormat.Value = {
    s.map(valueOfOrDefault).getOrElse(CsvFormat.QualityAssurance)
  }
}
