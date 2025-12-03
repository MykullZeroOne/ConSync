package com.consync.client.confluence.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response for content list queries.
 */
@Serializable
data class ContentResponse(
    val results: List<Page>,
    val start: Int = 0,
    val limit: Int = 25,
    val size: Int = 0,
    @SerialName("_links")
    val links: ContentLinks? = null
)

/**
 * Links for content response pagination.
 */
@Serializable
data class ContentLinks(
    val base: String? = null,
    val context: String? = null,
    val next: String? = null,
    val prev: String? = null,
    val self: String? = null
)
