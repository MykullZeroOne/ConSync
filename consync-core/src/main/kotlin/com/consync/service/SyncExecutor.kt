package com.consync.service

import com.consync.client.confluence.ConfluenceClient
import com.consync.client.confluence.model.*
import com.consync.config.Config
import com.consync.core.converter.ConfluenceConverter
import com.consync.model.*
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Executes sync plans by making Confluence API calls.
 *
 * Handles the actual creation, update, and deletion of pages
 * in Confluence based on a generated sync plan.
 */
class SyncExecutor(
    private val config: Config,
    private val client: ConfluenceClient,
    private val converter: ConfluenceConverter,
    private val stateManager: SyncStateManager
) {
    private val logger = LoggerFactory.getLogger(SyncExecutor::class.java)

    /**
     * Execute a sync plan.
     *
     * @param plan The sync plan to execute
     * @param dryRun If true, don't actually make changes
     * @param state Current sync state
     * @return Result of the sync execution
     */
    suspend fun execute(
        plan: SyncPlan,
        dryRun: Boolean,
        state: SyncState
    ): SyncResult {
        val startedAt = Instant.now()

        if (dryRun) {
            logger.info("Dry-run mode: showing what would be done")
            return SyncResult.dryRun(plan, startedAt)
        }

        if (!plan.hasChanges) {
            logger.info("No changes to sync")
            return SyncResult.success(emptyList(), startedAt, state)
        }

        logger.info(
            "Executing sync plan: {} creates, {} updates, {} deletes, {} moves",
            plan.createCount, plan.updateCount, plan.deleteCount, plan.moveCount
        )

        val actionResults = mutableListOf<ActionResult>()
        var currentState = state

        // Track created page IDs for parent resolution
        val createdPageIds = mutableMapOf<String, String>()

        try {
            for (action in plan.actions) {
                when (action.type) {
                    SyncActionType.CREATE -> {
                        val result = executeCreate(action, currentState, createdPageIds)
                        actionResults.add(result)

                        if (result.success && result.confluenceId != null) {
                            createdPageIds[action.relativePath] = result.confluenceId
                            currentState = updateStateAfterAction(currentState, action, result)
                        }
                    }
                    SyncActionType.UPDATE -> {
                        val result = executeUpdate(action, currentState)
                        actionResults.add(result)

                        if (result.success) {
                            currentState = updateStateAfterAction(currentState, action, result)
                        }
                    }
                    SyncActionType.DELETE -> {
                        val result = executeDelete(action)
                        actionResults.add(result)

                        if (result.success) {
                            currentState = currentState.withoutPage(action.relativePath)
                        }
                    }
                    SyncActionType.MOVE -> {
                        val result = executeMove(action, currentState, createdPageIds)
                        actionResults.add(result)

                        if (result.success) {
                            currentState = updateStateAfterAction(currentState, action, result)
                        }
                    }
                    SyncActionType.SKIP -> {
                        actionResults.add(ActionResult.skipped(action))
                    }
                }
            }

            // Save final state
            val finalState = currentState.withLastSync()
            stateManager.save(finalState)

            return SyncResult.success(actionResults, startedAt, finalState)
        } catch (e: Exception) {
            logger.error("Sync execution failed: {}", e.message)
            // Save partial state even on failure
            try {
                stateManager.save(currentState)
            } catch (saveError: Exception) {
                logger.error("Failed to save partial state: {}", saveError.message)
            }
            return SyncResult.failure(actionResults, startedAt, e.message ?: "Unknown error")
        }
    }

    /**
     * Execute a CREATE action.
     */
    private suspend fun executeCreate(
        action: SyncAction,
        state: SyncState,
        createdPageIds: Map<String, String>
    ): ActionResult {
        val pageNode = action.pageNode ?: return ActionResult.failure(action, "No page node for create action")

        try {
            logger.info("Creating page: {} ({})", action.title, action.relativePath)

            // Resolve parent ID (may be a newly created page)
            val parentId = resolveParentId(action, state, createdPageIds)

            // Convert content
            val content = pageNode.document?.content ?: ""
            val storageFormat = converter.convert(content)

            // Create page request
            val request = CreatePageRequest(
                title = action.title,
                space = SpaceReference(key = config.space.key),
                body = Body(storage = Storage(value = storageFormat)),
                ancestors = parentId?.let { listOf(PageReference(id = it)) }
            )

            val createdPage = client.createPage(request)
            logger.info("Created page '{}' with ID: {}", action.title, createdPage.id)

            return ActionResult.success(
                action = action,
                confluenceId = createdPage.id,
                contentHash = action.contentHash
            )
        } catch (e: Exception) {
            logger.error("Failed to create page '{}': {}", action.title, e.message)
            return ActionResult.failure(action, e.message ?: "Create failed")
        }
    }

    /**
     * Execute an UPDATE action.
     */
    private suspend fun executeUpdate(
        action: SyncAction,
        state: SyncState
    ): ActionResult {
        val pageNode = action.pageNode ?: return ActionResult.failure(action, "No page node for update action")
        val confluenceId = action.confluenceId ?: return ActionResult.failure(action, "No Confluence ID for update action")

        try {
            logger.info("Updating page: {} ({})", action.title, action.relativePath)

            // Get current page version
            val currentPage = client.getPage(confluenceId, listOf("version"))
            val newVersion = currentPage.version?.number?.plus(1) ?: 1

            // Convert content
            val content = pageNode.document?.content ?: ""
            val storageFormat = converter.convert(content)

            // Update page request
            val request = UpdatePageRequest(
                id = confluenceId,
                title = action.title,
                space = SpaceReference(key = config.space.key),
                body = Body(storage = Storage(value = storageFormat)),
                version = VersionUpdate(number = newVersion),
                ancestors = action.parentId?.let { listOf(PageReference(id = it)) }
            )

            val updatedPage = client.updatePage(confluenceId, request)
            logger.info("Updated page '{}' to version {}", action.title, updatedPage.version?.number ?: newVersion)

            return ActionResult.success(
                action = action,
                confluenceId = confluenceId,
                contentHash = action.contentHash
            )
        } catch (e: Exception) {
            logger.error("Failed to update page '{}': {}", action.title, e.message)
            return ActionResult.failure(action, e.message ?: "Update failed")
        }
    }

    /**
     * Execute a DELETE action.
     */
    private suspend fun executeDelete(action: SyncAction): ActionResult {
        val confluenceId = action.confluenceId ?: return ActionResult.failure(action, "No Confluence ID for delete action")

        try {
            logger.info("Deleting page: {} (id: {})", action.title, confluenceId)

            client.deletePage(confluenceId)
            logger.info("Deleted page '{}'", action.title)

            return ActionResult.success(action = action, confluenceId = null)
        } catch (e: Exception) {
            logger.error("Failed to delete page '{}': {}", action.title, e.message)
            return ActionResult.failure(action, e.message ?: "Delete failed")
        }
    }

    /**
     * Execute a MOVE action.
     */
    private suspend fun executeMove(
        action: SyncAction,
        state: SyncState,
        createdPageIds: Map<String, String>
    ): ActionResult {
        val confluenceId = action.confluenceId ?: return ActionResult.failure(action, "No Confluence ID for move action")
        val newParentId = resolveParentId(action, state, createdPageIds)
            ?: return ActionResult.failure(action, "Cannot resolve new parent ID for move action")

        try {
            logger.info("Moving page: {} to parent {}", action.title, newParentId)

            client.movePage(confluenceId, newParentId)
            logger.info("Moved page '{}' to new parent", action.title)

            return ActionResult.success(
                action = action,
                confluenceId = confluenceId,
                contentHash = action.contentHash
            )
        } catch (e: Exception) {
            logger.error("Failed to move page '{}': {}", action.title, e.message)
            return ActionResult.failure(action, e.message ?: "Move failed")
        }
    }

    /**
     * Resolve parent ID, checking both state and newly created pages.
     */
    private fun resolveParentId(
        action: SyncAction,
        state: SyncState,
        createdPageIds: Map<String, String>
    ): String? {
        // First, use the action's specified parent ID
        if (action.parentId != null) {
            return action.parentId
        }

        // Check if parent is a newly created page
        val pageNode = action.pageNode ?: return config.space.rootPageId
        val parentNode = pageNode.parent ?: return config.space.rootPageId

        // Check in created pages first
        val createdParentId = createdPageIds[parentNode.path.toString()]
        if (createdParentId != null) {
            return createdParentId
        }

        // Check in existing state
        val parentState = state.getPage(parentNode.path.toString())
        return parentState?.confluenceId ?: config.space.rootPageId
    }

    /**
     * Update sync state after a successful action.
     */
    private fun updateStateAfterAction(
        state: SyncState,
        action: SyncAction,
        result: ActionResult
    ): SyncState {
        val pageState = PageState.create(
            confluenceId = result.confluenceId ?: action.confluenceId ?: "",
            contentHash = result.contentHash ?: action.contentHash ?: "",
            title = action.title,
            parentId = action.parentId
        )
        return state.withPage(action.relativePath, pageState)
    }
}
