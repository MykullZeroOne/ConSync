package com.consync.client.confluence.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Confluence space.
 */
@Serializable
data class Space(
    val id: Long,
    val key: String,
    val name: String,
    val type: String? = null,
    val status: String? = null,
    @SerialName("_links")
    val links: SpaceLinks? = null
)

@Serializable
data class SpaceLinks(
    val webui: String? = null,
    val self: String? = null
)
