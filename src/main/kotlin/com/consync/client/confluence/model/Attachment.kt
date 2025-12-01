package com.consync.client.confluence.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Confluence attachment.
 */
@Serializable
data class Attachment(
    val id: String,
    val type: String = "attachment",
    val status: String = "current",
    val title: String,
    val version: Version? = null,
    val extensions: AttachmentExtensions? = null,
    @SerialName("_links")
    val links: AttachmentLinks? = null
)

/**
 * Attachment extension metadata.
 */
@Serializable
data class AttachmentExtensions(
    val mediaType: String? = null,
    val fileSize: Long? = null,
    val comment: String? = null
)

/**
 * Attachment links.
 */
@Serializable
data class AttachmentLinks(
    val webui: String? = null,
    val download: String? = null,
    val self: String? = null
)

/**
 * Response for attachment list queries.
 */
@Serializable
data class AttachmentResponse(
    val results: List<Attachment>,
    val start: Int = 0,
    val limit: Int = 25,
    val size: Int = 0
)
