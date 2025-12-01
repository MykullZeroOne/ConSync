package com.consync.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Persistent sync state tracking page mappings between local files and Confluence.
 *
 * This state is stored locally (typically in .consync/state.json) to enable
 * incremental syncing and change detection without API calls.
 */
@Serializable
data class SyncState(
    /** State file format version */
    val version: Int = CURRENT_VERSION,
    /** Space key this state is for */
    val spaceKey: String,
    /** Root page ID in Confluence */
    val rootPageId: String? = null,
    /** Timestamp of last successful sync (ISO-8601) */
    val lastSync: String? = null,
    /** Map of relative path to page state */
    val pages: Map<String, PageState> = emptyMap()
) {
    /**
     * Get page state by relative path.
     */
    fun getPage(relativePath: String): PageState? = pages[relativePath]

    /**
     * Get page state by Confluence ID.
     */
    fun getPageById(confluenceId: String): PageState? =
        pages.values.find { it.confluenceId == confluenceId }

    /**
     * Check if a page exists in the state.
     */
    fun hasPage(relativePath: String): Boolean = pages.containsKey(relativePath)

    /**
     * Check if content has changed based on hash.
     */
    fun hasContentChanged(relativePath: String, newHash: String): Boolean {
        val existing = pages[relativePath] ?: return true
        return existing.contentHash != newHash
    }

    /**
     * Create a new state with an added/updated page.
     */
    fun withPage(relativePath: String, pageState: PageState): SyncState =
        copy(pages = pages + (relativePath to pageState))

    /**
     * Create a new state with a page removed.
     */
    fun withoutPage(relativePath: String): SyncState =
        copy(pages = pages - relativePath)

    /**
     * Create a new state with updated last sync time.
     */
    fun withLastSync(instant: Instant = Instant.now()): SyncState =
        copy(lastSync = instant.toString())

    /**
     * Get all tracked relative paths.
     */
    fun trackedPaths(): Set<String> = pages.keys

    /**
     * Get all tracked Confluence IDs.
     */
    fun trackedIds(): Set<String> = pages.values.mapNotNull { it.confluenceId }.toSet()

    /**
     * Serialize to JSON string.
     */
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val CURRENT_VERSION = 1

        private val json = Json {
            prettyPrint = true
            encodeDefaults = true
        }

        /**
         * Create an empty state for a space.
         */
        fun empty(spaceKey: String, rootPageId: String? = null) = SyncState(
            spaceKey = spaceKey,
            rootPageId = rootPageId
        )

        /**
         * Parse state from JSON string.
         */
        fun fromJson(jsonString: String): SyncState =
            json.decodeFromString(serializer(), jsonString)
    }
}

/**
 * State of a single synced page.
 */
@Serializable
data class PageState(
    /** Confluence page ID */
    val confluenceId: String?,
    /** SHA-256 hash of the converted content */
    val contentHash: String,
    /** Page title at last sync */
    val title: String,
    /** Parent Confluence ID at last sync */
    val parentId: String? = null,
    /** Confluence version number at last sync */
    val version: Int = 1,
    /** Last modification timestamp (ISO-8601) */
    val lastModified: String = Instant.now().toString(),
    /** Last sync timestamp (ISO-8601) */
    val lastSynced: String = Instant.now().toString()
) {
    /**
     * Create updated page state after a sync.
     */
    fun updated(
        contentHash: String = this.contentHash,
        title: String = this.title,
        parentId: String? = this.parentId,
        version: Int = this.version + 1
    ) = copy(
        contentHash = contentHash,
        title = title,
        parentId = parentId,
        version = version,
        lastModified = Instant.now().toString(),
        lastSynced = Instant.now().toString()
    )

    companion object {
        /**
         * Create new page state for a freshly created page.
         */
        fun create(
            confluenceId: String,
            contentHash: String,
            title: String,
            parentId: String?
        ) = PageState(
            confluenceId = confluenceId,
            contentHash = contentHash,
            title = title,
            parentId = parentId,
            version = 1
        )
    }
}
