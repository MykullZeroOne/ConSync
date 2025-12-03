package com.consync.client.confluence.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Confluence page (content of type "page").
 */
@Serializable
data class Page(
    val id: String,
    val type: String = "page",
    val status: String = "current",
    val title: String,
    val space: SpaceReference? = null,
    val version: Version? = null,
    val body: Body? = null,
    val ancestors: List<PageReference>? = null,
    val children: Children? = null,
    @SerialName("_links")
    val links: PageLinks? = null,
    @SerialName("_expandable")
    val expandable: Expandable? = null
)

/**
 * Reference to a space (minimal info).
 */
@Serializable
data class SpaceReference(
    val key: String,
    val id: Long? = null,
    val name: String? = null
)

/**
 * Page version information.
 */
@Serializable
data class Version(
    val number: Int,
    val minorEdit: Boolean = false,
    val message: String? = null,
    val by: User? = null,
    @SerialName("when")
    val timestamp: String? = null
)

/**
 * User information.
 */
@Serializable
data class User(
    val type: String? = null,
    val accountId: String? = null,
    val accountType: String? = null,
    val email: String? = null,
    val publicName: String? = null,
    val displayName: String? = null
)

/**
 * Page body content.
 */
@Serializable
data class Body(
    val storage: Storage? = null,
    val view: ViewBody? = null
)

/**
 * Storage format body (XHTML).
 */
@Serializable
data class Storage(
    val value: String,
    val representation: String = "storage"
)

/**
 * View format body (rendered HTML).
 */
@Serializable
data class ViewBody(
    val value: String? = null,
    val representation: String? = null
)

/**
 * Reference to a page (for ancestors/children).
 */
@Serializable
data class PageReference(
    val id: String,
    val type: String? = null,
    val status: String? = null,
    val title: String? = null
)

/**
 * Children container.
 */
@Serializable
data class Children(
    val page: ChildPages? = null
)

/**
 * Child pages result.
 */
@Serializable
data class ChildPages(
    val results: List<Page>? = null,
    val start: Int? = null,
    val limit: Int? = null,
    val size: Int? = null
)

/**
 * Page links.
 */
@Serializable
data class PageLinks(
    val webui: String? = null,
    val edit: String? = null,
    val tinyui: String? = null,
    val self: String? = null,
    val base: String? = null,
    val context: String? = null
)

/**
 * Expandable fields indicator.
 */
@Serializable
data class Expandable(
    val children: String? = null,
    val ancestors: String? = null,
    val body: String? = null,
    val version: String? = null,
    val space: String? = null
)
