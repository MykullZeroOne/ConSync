package com.consync.service

import com.consync.model.PageState
import com.consync.model.SyncState
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Manages sync state persistence.
 *
 * Handles loading and saving the sync state file that tracks
 * the mapping between local files and Confluence pages.
 */
class SyncStateManager(
    private val stateFilePath: Path
) {
    private val logger = LoggerFactory.getLogger(SyncStateManager::class.java)

    /**
     * Load sync state from the state file.
     *
     * @param spaceKey The space key (used if creating new state)
     * @param rootPageId The root page ID (used if creating new state)
     * @return The loaded state, or a new empty state if file doesn't exist
     */
    fun load(spaceKey: String, rootPageId: String? = null): SyncState {
        if (!stateFilePath.exists()) {
            logger.debug("State file not found at {}, creating new state", stateFilePath)
            return SyncState.empty(spaceKey, rootPageId)
        }

        return try {
            val json = stateFilePath.readText()
            val state = SyncState.fromJson(json)

            // Validate state matches expected space
            if (state.spaceKey != spaceKey) {
                logger.warn(
                    "State file space key '{}' doesn't match configured space '{}'. Creating new state.",
                    state.spaceKey, spaceKey
                )
                return SyncState.empty(spaceKey, rootPageId)
            }

            // Check version compatibility
            if (state.version > SyncState.CURRENT_VERSION) {
                logger.warn(
                    "State file version {} is newer than supported version {}. Creating new state.",
                    state.version, SyncState.CURRENT_VERSION
                )
                return SyncState.empty(spaceKey, rootPageId)
            }

            logger.info(
                "Loaded sync state with {} tracked pages, last sync: {}",
                state.pages.size, state.lastSync ?: "never"
            )
            state
        } catch (e: Exception) {
            logger.error("Failed to load state file: {}. Creating new state.", e.message)
            SyncState.empty(spaceKey, rootPageId)
        }
    }

    /**
     * Save sync state to the state file.
     *
     * @param state The state to save
     */
    fun save(state: SyncState) {
        try {
            // Ensure parent directory exists
            val parentDir = stateFilePath.parent
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir)
                logger.debug("Created state directory: {}", parentDir)
            }

            // Write state file
            val json = state.toJson()
            Files.writeString(
                stateFilePath,
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )

            logger.info("Saved sync state with {} tracked pages to {}", state.pages.size, stateFilePath)
        } catch (e: Exception) {
            logger.error("Failed to save state file: {}", e.message)
            throw SyncStateException("Failed to save sync state: ${e.message}", e)
        }
    }

    /**
     * Update state with a synced page and save.
     *
     * @param state Current state
     * @param relativePath Relative path of the synced page
     * @param pageState New page state
     * @return Updated state
     */
    fun updatePage(state: SyncState, relativePath: String, pageState: PageState): SyncState {
        val updatedState = state.withPage(relativePath, pageState)
        save(updatedState)
        return updatedState
    }

    /**
     * Remove a page from state and save.
     *
     * @param state Current state
     * @param relativePath Relative path of the page to remove
     * @return Updated state
     */
    fun removePage(state: SyncState, relativePath: String): SyncState {
        val updatedState = state.withoutPage(relativePath)
        save(updatedState)
        return updatedState
    }

    /**
     * Delete the state file (for reset operations).
     */
    fun reset() {
        if (stateFilePath.exists()) {
            Files.delete(stateFilePath)
            logger.info("Deleted state file: {}", stateFilePath)
        }
    }

    /**
     * Check if state file exists.
     */
    fun exists(): Boolean = stateFilePath.exists()

    companion object {
        /** Default state file name */
        const val DEFAULT_STATE_FILE = ".consync/state.json"

        /**
         * Create a state manager for a content directory.
         */
        fun forDirectory(contentDir: Path, stateFileName: String = DEFAULT_STATE_FILE): SyncStateManager {
            val stateFilePath = contentDir.resolve(stateFileName)
            return SyncStateManager(stateFilePath)
        }
    }
}

/**
 * Exception thrown when state operations fail.
 */
class SyncStateException(message: String, cause: Throwable? = null) : Exception(message, cause)
