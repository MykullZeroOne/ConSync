package com.consync.service

import com.consync.client.confluence.ConfluenceClient
import com.consync.client.confluence.ConfluenceClientFactory
import com.consync.client.filesystem.FileScanner
import com.consync.config.Config
import com.consync.core.converter.ConfluenceConverter
import com.consync.core.hierarchy.HierarchyBuilder
import com.consync.core.hierarchy.PageNode
import com.consync.core.markdown.FrontmatterParser
import com.consync.core.markdown.MarkdownParser
import com.consync.model.SyncPlan
import com.consync.model.SyncResult
import com.consync.model.SyncState
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Main sync service orchestrating the entire sync process.
 *
 * This service coordinates all the components to:
 * 1. Scan local markdown files
 * 2. Build page hierarchy
 * 3. Load and compare with sync state
 * 4. Generate sync plan
 * 5. Execute sync (or show dry-run)
 * 6. Save updated state
 */
class SyncService(
    private val config: Config,
    private val contentDir: Path,
    private val client: ConfluenceClient,
    private val stateManager: SyncStateManager
) {
    private val logger = LoggerFactory.getLogger(SyncService::class.java)

    private val fileScanner = FileScanner(
        includePatterns = config.files.include,
        excludePatterns = config.files.exclude
    )

    private val frontmatterParser = FrontmatterParser()
    private val markdownParser = MarkdownParser(
        titleSource = config.content.titleSource,
        frontmatterParser = frontmatterParser
    )

    private val hierarchyBuilder = HierarchyBuilder(
        indexFileName = config.files.indexFile
    )

    private val converter = ConfluenceConverter(
        tocConfig = config.content.toc
    )

    private val diffService = DiffService(config, converter)

    private val executor = SyncExecutor(config, client, converter, stateManager)

    /**
     * Perform a full sync operation.
     *
     * @param dryRun If true, show what would be done without making changes
     * @param force If true, update all pages regardless of content hash
     * @return Result of the sync operation
     */
    suspend fun sync(dryRun: Boolean = false, force: Boolean = false): SyncResult {
        logger.info("Starting sync from {} to space '{}'", contentDir, config.space.key)

        // Step 1: Scan local files
        logger.info("Scanning local markdown files...")
        val markdownFiles = fileScanner.scan(contentDir)
        logger.info("Found {} markdown files", markdownFiles.size)

        if (markdownFiles.isEmpty()) {
            logger.warn("No markdown files found matching include patterns")
            return SyncResult.success(
                actionResults = emptyList(),
                startedAt = java.time.Instant.now(),
                updatedState = stateManager.load(config.space.key, config.space.rootPageId)
            )
        }

        // Step 2: Parse markdown files
        logger.info("Parsing markdown documents...")
        val documents = markdownFiles.mapNotNull { file ->
            try {
                markdownParser.parse(file, contentDir)
            } catch (e: Exception) {
                logger.error("Failed to parse {}: {}", file, e.message)
                null
            }
        }
        logger.info("Successfully parsed {} documents", documents.size)

        // Step 3: Build hierarchy
        logger.info("Building page hierarchy...")
        val rootNode = hierarchyBuilder.build(documents, contentDir)
        val pageCount = countPages(rootNode)
        logger.info("Built hierarchy with {} pages", pageCount)

        // Step 4: Load sync state
        logger.info("Loading sync state...")
        val rootPageId = resolveRootPageId()
        val syncState = stateManager.load(config.space.key, rootPageId)
        logger.info("Loaded state with {} tracked pages", syncState.pages.size)

        // Step 5: Generate sync plan
        logger.info("Generating sync plan...")
        val plan = diffService.generatePlan(
            rootNode = rootNode,
            syncState = syncState,
            rootPageId = rootPageId,
            force = force
        )

        logPlanSummary(plan)

        // Step 6: Execute sync (or dry-run)
        if (dryRun) {
            logger.info("Dry-run mode - no changes will be made")
            println(plan.summary())
            println()
            println(plan.details())
            return SyncResult.dryRun(plan, java.time.Instant.now())
        }

        logger.info("Executing sync plan...")
        val result = executor.execute(plan, dryRun = false, state = syncState)

        logSyncResult(result)

        return result
    }

    /**
     * Generate a sync plan without executing it.
     *
     * @param force If true, mark all pages for update
     * @return The generated sync plan
     */
    suspend fun plan(force: Boolean = false): SyncPlan {
        logger.info("Generating sync plan...")

        val markdownFiles = fileScanner.scan(contentDir)
        val documents = markdownFiles.mapNotNull { file ->
            try {
                markdownParser.parse(file, contentDir)
            } catch (e: Exception) {
                logger.error("Failed to parse {}: {}", file, e.message)
                null
            }
        }

        val rootNode = hierarchyBuilder.build(documents, contentDir)
        val rootPageId = resolveRootPageId()
        val syncState = stateManager.load(config.space.key, rootPageId)

        return diffService.generatePlan(
            rootNode = rootNode,
            syncState = syncState,
            rootPageId = rootPageId,
            force = force
        )
    }

    /**
     * Get current sync status.
     *
     * @return Status information
     */
    fun status(): SyncStatus {
        val syncState = stateManager.load(config.space.key, config.space.rootPageId)
        val markdownFiles = fileScanner.scan(contentDir)

        return SyncStatus(
            spaceKey = config.space.key,
            contentDir = contentDir.toString(),
            localFileCount = markdownFiles.size,
            trackedPageCount = syncState.pages.size,
            lastSync = syncState.lastSync,
            stateFileExists = stateManager.exists()
        )
    }

    /**
     * Reset sync state (deletes state file).
     */
    fun reset() {
        logger.info("Resetting sync state")
        stateManager.reset()
    }

    /**
     * Resolve the root page ID from config.
     */
    private suspend fun resolveRootPageId(): String? {
        // If ID is configured, use it directly
        config.space.rootPageId?.let { return it }

        // If title is configured, look it up
        config.space.rootPageTitle?.let { title ->
            try {
                val page = client.getPageByTitle(config.space.key, title)
                return page?.id
            } catch (e: Exception) {
                logger.warn("Could not find root page by title '{}': {}", title, e.message)
            }
        }

        return null
    }

    private fun countPages(node: PageNode): Int {
        var count = if (!node.isVirtual) 1 else 0
        node.children.forEach { count += countPages(it) }
        return count
    }

    private fun logPlanSummary(plan: SyncPlan) {
        if (plan.hasChanges) {
            logger.info(
                "Sync plan: {} creates, {} updates, {} deletes, {} moves, {} unchanged",
                plan.createCount, plan.updateCount, plan.deleteCount, plan.moveCount, plan.skipCount
            )
        } else {
            logger.info("No changes detected - everything is in sync")
        }
    }

    private fun logSyncResult(result: SyncResult) {
        if (result.success) {
            logger.info(
                "Sync completed: {} created, {} updated, {} deleted, {} moved ({} ms)",
                result.createdCount, result.updatedCount, result.deletedCount, result.movedCount,
                result.duration.toMillis()
            )
        } else {
            logger.error("Sync failed: {}", result.errorMessage)
            if (result.failedCount > 0) {
                logger.error("{} actions failed", result.failedCount)
            }
        }
    }

    companion object {
        /**
         * Create a sync service from configuration.
         *
         * @param config The application configuration
         * @param contentDir The directory containing markdown files
         * @return Configured SyncService instance
         */
        fun create(config: Config, contentDir: Path): SyncService {
            val client = ConfluenceClientFactory.create(config.confluence)
            val stateManager = SyncStateManager.forDirectory(
                contentDir,
                config.sync.stateFile
            )

            return SyncService(config, contentDir, client, stateManager)
        }
    }
}

/**
 * Status information about the sync state.
 */
data class SyncStatus(
    val spaceKey: String,
    val contentDir: String,
    val localFileCount: Int,
    val trackedPageCount: Int,
    val lastSync: String?,
    val stateFileExists: Boolean
) {
    fun print() {
        println("Sync Status")
        println("===========")
        println("Space: $spaceKey")
        println("Content Directory: $contentDir")
        println("Local Files: $localFileCount")
        println("Tracked Pages: $trackedPageCount")
        println("Last Sync: ${lastSync ?: "never"}")
        println("State File: ${if (stateFileExists) "exists" else "not found"}")
    }
}
