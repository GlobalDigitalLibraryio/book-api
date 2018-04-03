package db.migration

import io.digitallibrary.bookapi.UnitSuite
import io.digitallibrary.language.model.LanguageTag

class V24__UseTwoLetteredLanguageCodesTest extends UnitSuite {

  val migration = new V24__UseTwoLetteredLanguageCodes

  test("that crowdin_project_id is converted properly") {

    // 'eng' should be reduced to 'en'
    val inTranslation = V24_InTranslation(id = 123L, fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("nob"), crowdinProjectId = "gdl-test-eng")
    migration.modifyProjectId(inTranslation) should equal(inTranslation.copy(crowdinProjectId = "gdl-test-en"))

    // 'nso' should remain 'nso'
    val inTranslation2 = V24_InTranslation(id = 321L, fromLanguage = LanguageTag("nso"), toLanguage = LanguageTag("nob"), crowdinProjectId = "gdl-test-nso")
    migration.modifyProjectId(inTranslation2) should equal(inTranslation2.copy(crowdinProjectId = "gdl-test-nso"))
  }

}
