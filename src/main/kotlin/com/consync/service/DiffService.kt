package com.consync.service

import com.consync.config.Config
import com.consync.core.converter.ConfluenceConverter
import com.consync.core.hierarchy.PageNode
import com.consync.model.*
import org.slf4j.LoggerFactory
import java.security.MessageDigest

/**
 * Service for computing differences between local files and Confluence state.
 *
 * Compares the local page hierarchy with the persisted sync state and
 * current Confluence state to determine what sync actions are needed.
 */
class DiffService(
    private val config: Config,
    private val converter: ConfluenceConverter
) {
    private val logger = LoggerFactory.getLogger(DiffService::class.java)

    /**
     * Generate a sync plan by comparing local pages with sync state.
     *
     * @param rootNode The root of the local page hierarchy
     * @param syncState The current sync state
     * @param confluencePages Map of relative path to Confluence page ID (current state)
     * @param force If true, update all pages regardless of content hash
     * @return A sync plan with all required actions
     */
    fun generatePlan(
        rootNode: PageNode,
        syncState: SyncState,
        confluencePages: Map<String, String> = emptyMap(),
        rootPageId: String?,
        force: Boolean = false
    ): SyncPlan {
        val actions = mutableListOf<SyncAction>()
        val processedPaths = mutableSetOf<String>()

        // Collect all local pages (flattened from hierarchy)
        val localPages = collectAllPages(rootNode)
        logger.debug("Found {} local pages to process", localPages.size)

        // Process each local page
        for (pageNode in localPages) {
            val relativePath = pageNode.path.toString()
            processedPaths.add(relativePath)

            val action = determineAction(
                pageNode = pageNode,
                syncState = syncState,
                confluencePages = confluencePages,
                rootPageId = rootPageId,
                force = force
            )
            actions.add(action)
        }

        // Check for orphaned pages (in state but not in local files)
        if (config.sync.deleteOrphans) {
            val orphanedPaths = syncState.trackedPaths() - processedPaths
            for (orphanedPath in orphanedPaths) {
                val pageState = syncState.getPage(orphanedPath)
                if (pageState?.confluenceId != null) {
                    logger.debug("Found orphaned page: {} (id: {})", orphanedPath, pageState.confluenceId)
                    actions.add(
                        SyncAction.delete(
                            confluenceId = pageState.confluenceId,
                            title = pageState.title,
                            relativePath = orphanedPath
                        )
                    )
                }
            }
        }

        // Sort actions: creates first (parents before children), then updates, then deletes
        val sortedActions = sortActions(actions, rootNode)

        return SyncPlan(
            actions = sortedActions,
            spaceKey = config.space.key,
            rootPageId = rootPageId,
            totalLocalPages = localPages.size,
            totalConfluencePages = syncState.pages.size
        )
    }

    /**
     * Determine the sync action needed for a single page.
     */
    private fun determineAction(
        pageNode: PageNode,
        syncState: SyncState,
        confluencePages: Map<String, String>,
        rootPageId: String?,
        force: Boolean
    ): SyncAction {
        val relativePath = pageNode.path.toString()
        val existingState = syncState.getPage(relativePath)

        // Calculate content hash
        val content = pageNode.document?.content ?: ""
        val convertedContent = converter.convert(content)
        val contentHash = hashContent(convertedContent)

        // Determine parent ID
        val parentId = resolveParentId(pageNode, syncState, rootPageId)

        // Case 1: New page (not in sync state)
        if (existingState == null) {
            logger.debug("Page '{}' is new, will create", relativePath)
            return SyncAction.create(
                pageNode = pageNode,
                parentId = parentId,
                contentHash = contentHash
            )
        }

        val confluenceId = existingState.confluenceId

        // Case 2: Page exists but Confluence ID is missing (sync state corrupted)
        if (confluenceId == null) {
            logger.debug("Page '{}' has no Confluence ID, will create", relativePath)
            return SyncAction.create(
                pageNode = pageNode,
                parentId = parentId,
                contentHash = contentHash
            )
        }

        // Case 3: Check if parent changed (needs move)
        val parentChanged = existingState.parentId != parentId
        if (parentChanged) {
            logger.debug("Page '{}' parent changed from {} to {}", relativePath, existingState.parentId, parentId)
            // We'll handle the move as part of the update
        }

        // Case 4: Force update
        if (force) {
            logger.debug("Page '{}' force update requested", relativePath)
            return SyncAction.update(
                pageNode = pageNode,
                confluenceId = confluenceId,
                parentId = parentId,
                contentHash = contentHash,
                reason = "Force update"
            )
        }

        // Case 5: Content changed
        if (existingState.contentHash != contentHash) {
            logger.debug("Page '{}' content changed", relativePath)
            return SyncAction.update(
                pageNode = pageNode,
                confluenceId = confluenceId,
                parentId = parentId,
                contentHash = contentHash,
                reason = if (parentChanged) "Content and parent changed" else "Content changed"
            )
        }

        // Case 6: Title changed
        if (existingState.title != pageNode.title) {
            logger.debug("Page '{}' title changed from '{}' to '{}'", relativePath, existingState.title, pageNode.title)
            return SyncAction.update(
                pageNode = pageNode,
                confluenceId = confluenceId,
                parentId = parentId,
                contentHash = contentHash,
                reason = "Title changed"
            )
        }

        // Case 7: Only parent changed (move only, not content update)
        if (parentChanged) {
            return SyncAction.move(
                pageNode = pageNode,
                confluenceId = confluenceId,
                newParentId = parentId,
                previousParentId = existingState.parentId
            )
        }

        // Case 8: No changes
        logger.debug("Page '{}' unchanged, skipping", relativePath)
        return SyncAction.skip(
            pageNode = pageNode,
            confluenceId = confluenceId,
            reason = "Unchanged"
        )
    }

    /**
     * Resolve the parent Confluence ID for a page.
     */
    private fun resolveParentId(
        pageNode: PageNode,
        syncState: SyncState,
        rootPageId: String?
    ): String? {
        // If page is at root level, use the configured root page
        val parentNode = pageNode.parent ?: return rootPageId

        // Look up parent's Confluence ID from sync state
        val parentState = syncState.getPage(parentNode.path.toString())
        return parentState?.confluenceId ?: rootPageId
    }

    /**
     * Collect all page nodes from the hierarchy tree.
     */
    private fun collectAllPages(rootNode: PageNode): List<PageNode> {
        val pages = mutableListOf<PageNode>()

        fun collect(node: PageNode) {
            // Only include non-virtual nodes (actual pages)
            if (!node.isVirtual) {
                pages.add(node)
            }
            node.children.forEach { collect(it) }
        }

        // Don't include the root itself if it's just a container
        rootNode.children.forEach { collect(it) }

        return pages
    }

    /**
     * Sort actions for proper execution order.
     *
     * Creates must happen before their children can reference them as parents.
     * Deletes should happen last (children before parents).
     */
    private fun sortActions(actions: List<SyncAction>, rootNode: PageNode): List<SyncAction> {
        val creates = actions.filter { it.type == SyncActionType.CREATE }
        val updates = actions.filter { it.type == SyncActionType.UPDATE }
        val moves = actions.filter { it.type == SyncActionType.MOVE }
        val skips = actions.filter { it.type == SyncActionType.SKIP }
        val deletes = actions.filter { it.type == SyncActionType.DELETE }

        // Sort creates by depth (parents first)
        val sortedCreates = creates.sortedBy { action ->
            action.pageNode?.depth ?: 0
        }

        // Sort deletes by depth reversed (children first)
        val sortedDeletes = deletes.sortedByDescending { action ->
            action.relativePath.count { it == '/' }
        }

        return sortedCreates + updates + moves + skips + sortedDeletes
    }

    /**
     * Calculate SHA-256 hash of content.
     */
    private fun hashContent(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        /**
         * Create a diff service from configuration.
         */
        fun create(config: Config): DiffService {
            val converter = ConfluenceConverter(
                tocConfig = config.content.toc
            )
            return DiffService(config, converter)
        }
    }
}
