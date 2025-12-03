package com.consync.client.confluence.model

import kotlinx.serialization.Serializable

/**
 * Request body for creating a new page.
 */
@Serializable
data class CreatePageRequest(
    val type: String = "page",
    val title: String,
    val space: SpaceReference,
    val body: Body,
    val ancestors: List<PageReference>? = null,
    val status: String = "current"
)

/**
 * Request body for updating an existing page.
 */
@Serializable
data class UpdatePageRequest(
    val id: String,
    val type: String = "page",
    val title: String,
    val space: SpaceReference,
    val body: Body,
    val version: VersionUpdate,
    val ancestors: List<PageReference>? = null,
    val status: String = "current"
)

/**
 * Version information for update requests.
 */
@Serializable
data class VersionUpdate(
    val number: Int,
    val minorEdit: Boolean = false,
    val message: String? = null
)

/**
 * Request for moving a page to a new parent.
 */
@Serializable
data class MovePageRequest(
    val ancestors: List<PageReference>
)
