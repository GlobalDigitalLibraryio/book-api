/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import io.digitallibrary.language.model.LanguageTag


case class SearchResult[T] (totalCount: Long,
                            page: Int,
                            pageSize: Int,
                            language: LanguageTag,
                            results: Seq[T])

case class ReindexResult(totalIndexed: Int, millisUsed: Long)

