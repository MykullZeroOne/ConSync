package com.consync.service

import com.consync.model.PageState
import com.consync.model.SyncState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SyncStateManagerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var stateFilePath: Path
    private lateinit var stateManager: SyncStateManager

    @BeforeEach
    fun setup() {
        stateFilePath = tempDir.resolve(".consync/state.json")
        stateManager = SyncStateManager(stateFilePath)
    }

    @AfterEach
    fun cleanup() {
        if (Files.exists(stateFilePath)) {
            Files.delete(stateFilePath)
        }
    }

    @Test
    fun `should return empty state when file does not exist`() {
        val state = stateManager.load("DOCS", "root-123")

        assertEquals("DOCS", state.spaceKey)
        assertEquals("root-123", state.rootPageId)
        assertTrue(state.pages.isEmpty())
    }

    @Test
    fun `should save and load state`() {
        val pageState = PageState.create(
            confluenceId = "conf-123",
            contentHash = "sha256:abc",
            title = "Test Page",
            parentId = "root-123"
        )
        val state = SyncState.empty("DOCS", "root-123")
            .withPage("docs/test.md", pageState)
            .withLastSync()

        stateManager.save(state)
        val loaded = stateManager.load("DOCS", "root-123")

        assertEquals(1, loaded.pages.size)
        assertNotNull(loaded.lastSync)
        assertEquals("conf-123", loaded.getPage("docs/test.md")?.confluenceId)
    }

    @Test
    fun `should create parent directories when saving`() {
        val state = SyncState.empty("DOCS")

        stateManager.save(state)

        assertTrue(Files.exists(stateFilePath.parent))
        assertTrue(Files.exists(stateFilePath))
    }

    @Test
    fun `should update page in state`() {
        val initialState = SyncState.empty("DOCS")
        val pageState = PageState.create(
            confluenceId = "conf-123",
            contentHash = "sha256:abc",
            title = "Test",
            parentId = null
        )

        val updated = stateManager.updatePage(initialState, "test.md", pageState)

        assertEquals(1, updated.pages.size)
        assertTrue(Files.exists(stateFilePath))
    }

    @Test
    fun `should remove page from state`() {
        val pageState = PageState.create(
            confluenceId = "conf-123",
            contentHash = "sha256:abc",
            title = "Test",
            parentId = null
        )
        val initialState = SyncState.empty("DOCS").withPage("test.md", pageState)
        stateManager.save(initialState)

        val updated = stateManager.removePage(initialState, "test.md")

        assertTrue(updated.pages.isEmpty())
    }

    @Test
    fun `should reset state file`() {
        val state = SyncState.empty("DOCS")
        stateManager.save(state)
        assertTrue(stateManager.exists())

        stateManager.reset()

        assertFalse(stateManager.exists())
    }

    @Test
    fun `should return new state when space key mismatch`() {
        val wrongSpaceState = SyncState.empty("OTHER")
        stateManager.save(wrongSpaceState)

        val loaded = stateManager.load("DOCS", null)

        assertEquals("DOCS", loaded.spaceKey)
        assertTrue(loaded.pages.isEmpty())
    }

    @Test
    fun `should handle corrupted state file`() {
        Files.createDirectories(stateFilePath.parent)
        Files.writeString(stateFilePath, "{ invalid json }")

        val loaded = stateManager.load("DOCS", null)

        assertEquals("DOCS", loaded.spaceKey)
        assertTrue(loaded.pages.isEmpty())
    }

    @Test
    fun `should check if state file exists`() {
        assertFalse(stateManager.exists())

        stateManager.save(SyncState.empty("DOCS"))

        assertTrue(stateManager.exists())
    }

    @Test
    fun `should create state manager for directory`() {
        val manager = SyncStateManager.forDirectory(tempDir)

        val state = SyncState.empty("DOCS")
        manager.save(state)

        val expectedPath = tempDir.resolve(".consync/state.json")
        assertTrue(Files.exists(expectedPath))
    }

    @Test
    fun `should create state manager with custom filename`() {
        val manager = SyncStateManager.forDirectory(tempDir, "custom-state.json")

        val state = SyncState.empty("DOCS")
        manager.save(state)

        val expectedPath = tempDir.resolve("custom-state.json")
        assertTrue(Files.exists(expectedPath))
    }
}
