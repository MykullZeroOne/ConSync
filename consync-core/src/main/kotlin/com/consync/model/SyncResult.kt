package com.consync.model

import java.time.Duration
import java.time.Instant

/**
 * Result of executing a sync plan.
 */
data class SyncResult(
    /** Whether the sync was successful overall */
    val success: Boolean,
    /** Individual action results */
    val actionResults: List<ActionResult>,
    /** When sync started */
    val startedAt: Instant,
    /** When sync completed */
    val completedAt: Instant,
    /** Updated sync state after execution */
    val updatedState: SyncState?,
    /** Error message if sync failed */
    val errorMessage: String? = null
) {
    /** Duration of the sync operation */
    val duration: Duration get() = Duration.between(startedAt, completedAt)

    /** Successful action results */
    val successfulActions: List<ActionResult> by lazy {
        actionResults.filter { it.success }
    }

    /** Failed action results */
    val failedActions: List<ActionResult> by lazy {
        actionResults.filter { !it.success }
    }

    /** Count of successful creates */
    val createdCount: Int get() = actionResults.count {
        it.success && it.actionType == SyncActionType.CREATE
    }

    /** Count of successful updates */
    val updatedCount: Int get() = actionResults.count {
        it.success && it.actionType == SyncActionType.UPDATE
    }

    /** Count of successful deletes */
    val deletedCount: Int get() = actionResults.count {
        it.success && it.actionType == SyncActionType.DELETE
    }

    /** Count of successful moves */
    val movedCount: Int get() = actionResults.count {
        it.success && it.actionType == SyncActionType.MOVE
    }

    /** Count of skipped pages */
    val skippedCount: Int get() = actionResults.count {
        it.actionType == SyncActionType.SKIP
    }

    /** Count of failed actions */
    val failedCount: Int get() = failedActions.size

    /** Total actions attempted (excluding skips) */
    val totalAttempted: Int get() = actionResults.count {
        it.actionType != SyncActionType.SKIP
    }

    /**
     * Get a summary of the sync result.
     */
    fun summary(): String = buildString {
        appendLine("Sync Result")
        appendLine("===========")
        appendLine("Status: ${if (success) "SUCCESS" else "FAILED"}")
        appendLine("Duration: ${duration.toMillis()}ms")
        appendLine()
        appendLine("Results:")
        appendLine("  Created: $createdCount")
        appendLine("  Updated: $updatedCount")
        appendLine("  Deleted: $deletedCount")
        appendLine("  Moved: $movedCount")
        appendLine("  Skipped: $skippedCount")
        appendLine("  Failed: $failedCount")

        if (failedActions.isNotEmpty()) {
            appendLine()
            appendLine("Failures:")
            failedActions.forEach { result ->
                appendLine("  - ${result.title}: ${result.errorMessage}")
            }
        }

        if (errorMessage != null) {
            appendLine()
            appendLine("Error: $errorMessage")
        }
    }

    companion object {
        /**
         * Create a successful result.
         */
        fun success(
            actionResults: List<ActionResult>,
            startedAt: Instant,
            updatedState: SyncState
        ) = SyncResult(
            success = actionResults.none { !it.success && it.actionType != SyncActionType.SKIP },
            actionResults = actionResults,
            startedAt = startedAt,
            completedAt = Instant.now(),
            updatedState = updatedState
        )

        /**
         * Create a failed result.
         */
        fun failure(
            actionResults: List<ActionResult>,
            startedAt: Instant,
            errorMessage: String
        ) = SyncResult(
            success = false,
            actionResults = actionResults,
            startedAt = startedAt,
            completedAt = Instant.now(),
            updatedState = null,
            errorMessage = errorMessage
        )

        /**
         * Create a dry-run result (no actual changes made).
         */
        fun dryRun(
            plan: SyncPlan,
            startedAt: Instant
        ) = SyncResult(
            success = true,
            actionResults = plan.actions.map { action ->
                ActionResult.dryRun(action)
            },
            startedAt = startedAt,
            completedAt = Instant.now(),
            updatedState = null
        )
    }
}

/**
 * Result of executing a single sync action.
 */
data class ActionResult(
    /** The action that was executed */
    val actionType: SyncActionType,
    /** Page title */
    val title: String,
    /** Relative path */
    val relativePath: String,
    /** Whether the action succeeded */
    val success: Boolean,
    /** Confluence page ID (after create/update) */
    val confluenceId: String? = null,
    /** New content hash */
    val contentHash: String? = null,
    /** Error message if failed */
    val errorMessage: String? = null,
    /** Whether this was a dry-run (no actual change) */
    val dryRun: Boolean = false
) {
    companion object {
        /**
         * Create a successful action result.
         */
        fun success(
            action: SyncAction,
            confluenceId: String?,
            contentHash: String? = null
        ) = ActionResult(
            actionType = action.type,
            title = action.title,
            relativePath = action.relativePath,
            success = true,
            confluenceId = confluenceId,
            contentHash = contentHash
        )

        /**
         * Create a failed action result.
         */
        fun failure(
            action: SyncAction,
            errorMessage: String
        ) = ActionResult(
            actionType = action.type,
            title = action.title,
            relativePath = action.relativePath,
            success = false,
            errorMessage = errorMessage
        )

        /**
         * Create a skipped action result.
         */
        fun skipped(action: SyncAction) = ActionResult(
            actionType = SyncActionType.SKIP,
            title = action.title,
            relativePath = action.relativePath,
            success = true,
            confluenceId = action.confluenceId
        )

        /**
         * Create a dry-run action result.
         */
        fun dryRun(action: SyncAction) = ActionResult(
            actionType = action.type,
            title = action.title,
            relativePath = action.relativePath,
            success = true,
            confluenceId = action.confluenceId,
            dryRun = true
        )
    }
}
