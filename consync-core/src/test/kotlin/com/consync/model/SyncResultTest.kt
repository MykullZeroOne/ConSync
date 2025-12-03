package com.consync.model

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SyncResultTest {

    @Test
    fun `should report success when all actions succeed`() {
        val results = listOf(
            ActionResult.success(createAction(SyncActionType.CREATE, "page1"), "conf-1"),
            ActionResult.success(createAction(SyncActionType.UPDATE, "page2"), "conf-2")
        )

        val syncResult = SyncResult.success(
            actionResults = results,
            startedAt = Instant.now().minusSeconds(5),
            updatedState = SyncState.empty("DOCS")
        )

        assertTrue(syncResult.success)
        assertEquals(1, syncResult.createdCount)
        assertEquals(1, syncResult.updatedCount)
        assertEquals(0, syncResult.failedCount)
    }

    @Test
    fun `should report failure when any action fails`() {
        val results = listOf(
            ActionResult.success(createAction(SyncActionType.CREATE, "page1"), "conf-1"),
            ActionResult.failure(createAction(SyncActionType.UPDATE, "page2"), "Permission denied")
        )

        val syncResult = SyncResult.success(
            actionResults = results,
            startedAt = Instant.now().minusSeconds(5),
            updatedState = SyncState.empty("DOCS")
        )

        assertFalse(syncResult.success)
        assertEquals(1, syncResult.failedCount)
    }

    @Test
    fun `should count actions by type`() {
        val results = listOf(
            ActionResult.success(createAction(SyncActionType.CREATE, "page1"), "conf-1"),
            ActionResult.success(createAction(SyncActionType.CREATE, "page2"), "conf-2"),
            ActionResult.success(createAction(SyncActionType.UPDATE, "page3"), "conf-3"),
            ActionResult.success(createAction(SyncActionType.DELETE, "page4"), null),
            ActionResult.skipped(createAction(SyncActionType.SKIP, "page5"))
        )

        val syncResult = SyncResult.success(
            actionResults = results,
            startedAt = Instant.now().minusSeconds(5),
            updatedState = SyncState.empty("DOCS")
        )

        assertEquals(2, syncResult.createdCount)
        assertEquals(1, syncResult.updatedCount)
        assertEquals(1, syncResult.deletedCount)
        assertEquals(1, syncResult.skippedCount)
        assertEquals(4, syncResult.totalAttempted)
    }

    @Test
    fun `should calculate duration`() {
        val startTime = Instant.now().minusSeconds(10)
        val syncResult = SyncResult.success(
            actionResults = emptyList(),
            startedAt = startTime,
            updatedState = SyncState.empty("DOCS")
        )

        assertTrue(syncResult.duration.toMillis() >= 0)
    }

    @Test
    fun `should generate summary`() {
        val results = listOf(
            ActionResult.success(createAction(SyncActionType.CREATE, "page1"), "conf-1"),
            ActionResult.failure(createAction(SyncActionType.UPDATE, "page2"), "Error message")
        )

        val syncResult = SyncResult.success(
            actionResults = results,
            startedAt = Instant.now().minusSeconds(5),
            updatedState = SyncState.empty("DOCS")
        )

        val summary = syncResult.summary()

        assertTrue(summary.contains("Sync Result"))
        assertTrue(summary.contains("Created: 1"))
        assertTrue(summary.contains("Failed: 1"))
        assertTrue(summary.contains("page2: Error message"))
    }

    @Test
    fun `should create dry-run result`() {
        val plan = SyncPlan(
            actions = listOf(createAction(SyncActionType.CREATE, "page1")),
            spaceKey = "DOCS",
            rootPageId = null,
            totalLocalPages = 1,
            totalConfluencePages = 0
        )

        val result = SyncResult.dryRun(plan, Instant.now())

        assertTrue(result.success)
        assertTrue(result.actionResults.all { it.dryRun })
    }

    @Test
    fun `should create failure result`() {
        val result = SyncResult.failure(
            actionResults = emptyList(),
            startedAt = Instant.now(),
            errorMessage = "Connection failed"
        )

        assertFalse(result.success)
        assertEquals("Connection failed", result.errorMessage)
    }

    private fun createAction(type: SyncActionType, title: String): SyncAction {
        return SyncAction(
            type = type,
            pageNode = null,
            confluenceId = if (type != SyncActionType.CREATE) "conf-id" else null,
            title = title,
            relativePath = "$title.md",
            parentId = null,
            reason = "Test"
        )
    }
}

class ActionResultTest {

    @Test
    fun `should create success result`() {
        val action = SyncAction(
            type = SyncActionType.CREATE,
            pageNode = null,
            confluenceId = null,
            title = "Test",
            relativePath = "test.md",
            parentId = null,
            reason = "New"
        )

        val result = ActionResult.success(action, "conf-123", "sha256:abc")

        assertTrue(result.success)
        assertEquals("conf-123", result.confluenceId)
        assertEquals("sha256:abc", result.contentHash)
        assertFalse(result.dryRun)
    }

    @Test
    fun `should create failure result`() {
        val action = SyncAction(
            type = SyncActionType.UPDATE,
            pageNode = null,
            confluenceId = "conf-123",
            title = "Test",
            relativePath = "test.md",
            parentId = null,
            reason = "Changed"
        )

        val result = ActionResult.failure(action, "Permission denied")

        assertFalse(result.success)
        assertEquals("Permission denied", result.errorMessage)
    }

    @Test
    fun `should create dry-run result`() {
        val action = SyncAction(
            type = SyncActionType.DELETE,
            pageNode = null,
            confluenceId = "conf-123",
            title = "Test",
            relativePath = "test.md",
            parentId = null,
            reason = "Orphaned"
        )

        val result = ActionResult.dryRun(action)

        assertTrue(result.success)
        assertTrue(result.dryRun)
    }
}
