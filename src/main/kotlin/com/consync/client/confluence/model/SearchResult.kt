package com.consync.client.confluence.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Search results from Confluence CQL query.
 */
@Serializable
data class SearchResult(
    val results: List<SearchResultItem>,
    val start: Int = 0,
    val limit: Int = 25,
    val size: Int = 0,
    val totalSize: Int? = null,
    @SerialName("_links")
    val links: SearchLinks? = null
)

/**
 * Individual search result item.
 */
@Serializable
data class SearchResultItem(
    val content: Page? = null,
    val title: String? = null,
    val excerpt: String? = null,
    val url: String? = null,
    val resultGlobalContainer: ResultContainer? = null,
    val lastModified: String? = null,
    val friendlyLastModified: String? = null
)

/**
 * Container info for search result.
 */
@Serializable
data class ResultContainer(
    val title: String? = null,
    val displayUrl: String? = null
)

/**
 * Search result links.
 */
@Serializable
data class SearchLinks(
    val base: String? = null,
    val context: String? = null,
    val next: String? = null,
    val self: String? = null
)
