package io.digitallibrary.bookapi.service.pdf

import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag

class PdfTest extends UnitSuite with TestEnvironment {

  test("that correct font is recognized") {
    val pdf = Pdf(LanguageTag("en"), Seq(TestData.Api.Chapter1), TestData.Domain.DefaultTranslation)
    pdf.fontDefinitions.get(LanguageTag("amh")) should equal(Some(pdf.FontDefinition("/NotoSansEthiopic.ttf", "Noto Sans Ethiopic")))
    pdf.fontDefinitions.get(LanguageTag("am")) should equal(Some(pdf.FontDefinition("/NotoSansEthiopic.ttf", "Noto Sans Ethiopic")))
    pdf.fontDefinitions.get(LanguageTag("km")) should equal(Some(pdf.FontDefinition("/NotoSansKhmer-Regular.ttf", "Noto Sans Khmer")))
    pdf.fontDefinitions.get(LanguageTag("khm")) should equal(Some(pdf.FontDefinition("/NotoSansKhmer-Regular.ttf", "Noto Sans Khmer")))
    pdf.fontDefinitions.get(LanguageTag("mai")) should equal(Some(pdf.FontDefinition("/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari")))
    pdf.fontDefinitions.get(LanguageTag("awa")) should equal(Some(pdf.FontDefinition("/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari")))
    pdf.fontDefinitions.get(LanguageTag("nob")) should be(None)

  }
}
