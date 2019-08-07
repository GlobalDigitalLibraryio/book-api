package io.digitallibrary.bookapi.service.pdf

import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag

class PdfV2Test extends UnitSuite with TestEnvironment{

  test("v2: that correct font is recognized") {
    val pdf = PdfV2(LanguageTag("en"), Seq(TestData.ApiV2.Chapter1), TestData.Domain.DefaultTranslation)
    pdf.fontDefinitions.get(LanguageTag("amh")) should equal(Some(pdf.FontDefinition("/NotoSansEthiopic.ttf", "Noto Sans Ethiopic")))
    pdf.fontDefinitions.get(LanguageTag("am")) should equal(Some(pdf.FontDefinition("/NotoSansEthiopic.ttf", "Noto Sans Ethiopic")))
    pdf.fontDefinitions.get(LanguageTag("km")) should equal(Some(pdf.FontDefinition("/NotoSansKhmer-Regular.ttf", "Noto Sans Khmer")))
    pdf.fontDefinitions.get(LanguageTag("khm")) should equal(Some(pdf.FontDefinition("/NotoSansKhmer-Regular.ttf", "Noto Sans Khmer")))
    pdf.fontDefinitions.get(LanguageTag("nob")) should be(None)
  }
}
