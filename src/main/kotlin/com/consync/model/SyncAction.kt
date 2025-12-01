package com.consync.model

import com.consync.core.hierarchy.PageNode

/**
 * Types of sync actions that can be performed.
 */
enum class SyncActionType {
    /** Create a new page in Confluence */
    CREATE,
    /** Update an existing page in Confluence */
    UPDATE,
    /** Delete a page from Confluence (orphan removal) */
    DELETE,
    /** Move a page to a different parent */
    MOVE,
    /** Skip - no action needed (unchanged) */
    SKIP
}

/**
 * Represents a single sync action to be performed.
 */
data class SyncAction(
    /** Type of action to perform */
    val type: SyncActionType,
    /** Local page node (null for DELETE actions) */
    val pageNode: PageNode?,
    /** Confluence page ID (null for CREATE actions) */
    val confluenceId: String?,
    /** Page title */
    val title: String,
    /** Relative path of the local file */
    val relativePath: String,
    /** Parent Confluence page ID */
    val parentId: String?,
    /** Reason for this action */
    val reason: String,
    /** Content hash for change detection */
    val contentHash: String? = null,
    /** Previous parent ID (for MOVE actions) */
    val previousParentId: String? = null
) {
    /**
     * Check if this action will modify Confluence.
     */
    fun isModifying(): Boolean = type != SyncActionType.SKIP

    /**
     * Get a human-readable description of this action.
     */
    fun describe(): String = when (type) {
        SyncActionType.CREATE -> "[CREATE] $title ($relativePath)"
        SyncActionType.UPDATE -> "[UPDATE] $title ($relativePath)"
        SyncActionType.DELETE -> "[DELETE] $title (id: $confluenceId)"
        SyncActionType.MOVE -> "[MOVE] $title to parent $parentId"
        SyncActionType.SKIP -> "[SKIP] $title ($reason)"
    }

    companion object {
        /**
         * Create a CREATE action for a new page.
         */
        fun create(
            pageNode: PageNode,
            parentId: String?,
            contentHash: String
        ) = SyncAction(
            type = SyncActionType.CREATE,
            pageNode = pageNode,
            confluenceId = null,
            title = pageNode.title,
            relativePath = pageNode.relativePath,
            parentId = parentId,
            reason = "New page",
            contentHash = contentHash
        )

        /**
         * Create an UPDATE action for a modified page.
         */
        fun update(
            pageNode: PageNode,
            confluenceId: String,
            parentId: String?,
            contentHash: String,
            reason: String = "Content changed"
        ) = SyncAction(
            type = SyncActionType.UPDATE,
            pageNode = pageNode,
            confluenceId = confluenceId,
            title = pageNode.title,
            relativePath = pageNode.relativePath,
            parentId = parentId,
            reason = reason,
            contentHash = contentHash
        )

        /**
         * Create a DELETE action for an orphaned page.
         */
        fun delete(
            confluenceId: String,
            title: String,
            relativePath: String
        ) = SyncAction(
            type = SyncActionType.DELETE,
            pageNode = null,
            confluenceId = confluenceId,
            title = title,
            relativePath = relativePath,
            parentId = null,
            reason = "Orphaned page"
        )

        /**
         * Create a MOVE action for a page that changed parents.
         */
        fun move(
            pageNode: PageNode,
            confluenceId: String,
            newParentId: String?,
            previousParentId: String?
        ) = SyncAction(
            type = SyncActionType.MOVE,
            pageNode = pageNode,
            confluenceId = confluenceId,
            title = pageNode.title,
            relativePath = pageNode.relativePath,
            parentId = newParentId,
            reason = "Parent changed",
            previousParentId = previousParentId
        )

        /**
         * Create a SKIP action for an unchanged page.
         */
        fun skip(
            pageNode: PageNode,
            confluenceId: String?,
            reason: String = "Unchanged"
        ) = SyncAction(
            type = SyncActionType.SKIP,
            pageNode = pageNode,
            confluenceId = confluenceId,
            title = pageNode.title,
            relativePath = pageNode.relativePath,
            parentId = null,
            reason = reason
        )
    }
}
