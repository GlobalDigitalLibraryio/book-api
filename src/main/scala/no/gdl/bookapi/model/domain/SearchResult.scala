/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain


case class SearchResult[T] (totalCount: Long,
                            page: Int,
                            pageSize: Int,
                            language: String,
                            results: Seq[T])


