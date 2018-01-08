/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import javax.servlet.http.HttpServletRequest

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.network.AuthUser
import no.gdl.bookapi.model.domain.InTranslation
import no.gdl.bookapi.{TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._

class WriteTranslationServiceTest extends UnitSuite with TestEnvironment {

  override val writeTranslationService = new WriteTranslationService

  test("that addUserToTranslation does not add the same user id twice") {
    val inTranslation = InTranslation(Some(1), Some(1), Seq("abc123"), 1, None, LanguageTag("eng"), LanguageTag("nob"), "123")

    val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzY29wZSI6InJvbGUxIHJvbGUyIHJvbGUzIiwiaHR0cHM6Ly9kaWdpdGFsbGlicmFyeS5pby9nZGxfaWQiOiJhYmMxMjMiLCJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL3VzZXJfbmFtZSI6IkRvbmFsZCBEdWNrIiwiaXNzIjoiaHR0cHM6Ly9zb21lLWRvbWFpbi8iLCJzdWIiOiJnb29nbGUtb2F1dGgyfDEyMyIsImF1ZCI6ImFiYyIsImV4cCI6MTQ4NjA3MDA2MywiaWF0IjoxNDg2MDM0MDYzfQ.gbMF8F1LLMUVroXbmStL02R6EPZjeZkbowseE5SAN9U"
    val request = mock[HttpServletRequest]
    when(request.getHeader("Authorization")).thenReturn(s"Bearer $token")
    AuthUser.set(request)

    val addUserToTranslation = writeTranslationService.addUserToTranslation(inTranslation)
    addUserToTranslation.isSuccess should be (true)

    verify(inTranslationRepository, never()).updateTranslation(any[InTranslation])

  }

}
