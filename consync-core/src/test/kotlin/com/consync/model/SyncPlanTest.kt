package com.consync.model

import com.consync.core.hierarchy.PageNode
import com.consync.core.markdown.MarkdownDocument
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SyncPlanTest {

    @Test
    fun `should count actions by type`() {
        val actions = listOf(
            createAction(SyncActionType.CREATE, "page1"),
            createAction(SyncActionType.CREATE, "page2"),
            createAction(SyncActionType.UPDATE, "page3"),
            createAction(SyncActionType.DELETE, "page4"),
            createAction(SyncActionType.SKIP, "page5")
        )

        val plan = SyncPlan(
            actions = actions,
            spaceKey = "DOCS",
            rootPageId = "root",
            totalLocalPages = 4,
            totalConfluencePages = 3
        )

        assertEquals(2, plan.createCount)
        assertEquals(1, plan.updateCount)
        assertEquals(1, plan.deleteCount)
        assertEquals(0, plan.moveCount)
        assertEquals(1, plan.skipCount)
    }

    @Test
    fun `should report has changes when modifying actions exist`() {
        val plan = SyncPlan(
            actions = listOf(createAction(SyncActionType.CREATE, "page1")),
            spaceKey = "DOCS",
            rootPageId = null,
            totalLocalPages = 1,
            totalConfluencePages = 0
        )

        assertTrue(plan.hasChanges)
        assertEquals(1, plan.modifyingCount)
    }

    @Test
    fun `should report no changes when only skips`() {
        val plan = SyncPlan(
            actions = listOf(createAction(SyncActionType.SKIP, "page1")),
            spaceKey = "DOCS",
            rootPageId = null,
            totalLocalPages = 1,
            totalConfluencePages = 1
        )

        assertFalse(plan.hasChanges)
        assertEquals(0, plan.modifyingCount)
    }

    @Test
    fun `should report empty when no actions`() {
        val plan = SyncPlan.empty("DOCS", null)

        assertTrue(plan.isEmpty)
        assertFalse(plan.hasChanges)
    }

    @Test
    fun `should generate summary`() {
        val plan = SyncPlan(
            actions = listOf(
                createAction(SyncActionType.CREATE, "page1"),
                createAction(SyncActionType.UPDATE, "page2")
            ),
            spaceKey = "DOCS",
            rootPageId = "root",
            totalLocalPages = 2,
            totalConfluencePages = 1
        )

        val summary = plan.summary()

        assertTrue(summary.contains("Space: DOCS"))
        assertTrue(summary.contains("Create: 1"))
        assertTrue(summary.contains("Update: 1"))
        assertTrue(summary.contains("Total changes: 2"))
    }

    @Test
    fun `should generate details`() {
        val plan = SyncPlan(
            actions = listOf(
                createAction(SyncActionType.CREATE, "New Page"),
                createAction(SyncActionType.UPDATE, "Updated Page"),
                createAction(SyncActionType.DELETE, "Deleted Page")
            ),
            spaceKey = "DOCS",
            rootPageId = null,
            totalLocalPages = 2,
            totalConfluencePages = 2
        )

        val details = plan.details()

        assertTrue(details.contains("Pages to CREATE:"))
        assertTrue(details.contains("+ New Page"))
        assertTrue(details.contains("Pages to UPDATE:"))
        assertTrue(details.contains("~ Updated Page"))
        assertTrue(details.contains("Pages to DELETE:"))
        assertTrue(details.contains("- Deleted Page"))
    }

    private fun createAction(type: SyncActionType, title: String): SyncAction {
        return SyncAction(
            type = type,
            pageNode = if (type != SyncActionType.DELETE) createMockPageNode(title) else null,
            confluenceId = if (type != SyncActionType.CREATE) "conf-id" else null,
            title = title,
            relativePath = "${title.lowercase().replace(" ", "-")}.md",
            parentId = null,
            reason = "Test"
        )
    }

    private fun createMockPageNode(title: String): PageNode {
        val pathStr = "${title.lowercase().replace(" ", "-")}.md"
        return PageNode(
            id = pathStr,
            title = title,
            path = java.nio.file.Paths.get(pathStr),
            document = null,
            isVirtual = false
        )
    }
}

class SyncActionTest {

    @Test
    fun `should create CREATE action`() {
        val pageNode = createMockPageNode("Test Page", "test.md")
        val action = SyncAction.create(pageNode, "parent-123", "sha256:abc")

        assertEquals(SyncActionType.CREATE, action.type)
        assertEquals("Test Page", action.title)
        assertEquals("test.md", action.relativePath)
        assertEquals("parent-123", action.parentId)
        assertEquals("sha256:abc", action.contentHash)
        assertTrue(action.isModifying())
    }

    @Test
    fun `should create UPDATE action`() {
        val pageNode = createMockPageNode("Test Page", "test.md")
        val action = SyncAction.update(pageNode, "conf-123", "parent-123", "sha256:def", "Content changed")

        assertEquals(SyncActionType.UPDATE, action.type)
        assertEquals("conf-123", action.confluenceId)
        assertEquals("Content changed", action.reason)
        assertTrue(action.isModifying())
    }

    @Test
    fun `should create DELETE action`() {
        val action = SyncAction.delete("conf-123", "Orphaned Page", "orphan.md")

        assertEquals(SyncActionType.DELETE, action.type)
        assertEquals("conf-123", action.confluenceId)
        assertEquals("Orphaned Page", action.title)
        assertTrue(action.isModifying())
    }

    @Test
    fun `should create SKIP action`() {
        val pageNode = createMockPageNode("Unchanged", "unchanged.md")
        val action = SyncAction.skip(pageNode, "conf-123", "No changes")

        assertEquals(SyncActionType.SKIP, action.type)
        assertEquals("No changes", action.reason)
        assertFalse(action.isModifying())
    }

    @Test
    fun `should describe CREATE action`() {
        val pageNode = createMockPageNode("New Page", "new.md")
        val action = SyncAction.create(pageNode, null, "hash")

        val description = action.describe()

        assertTrue(description.contains("[CREATE]"))
        assertTrue(description.contains("New Page"))
        assertTrue(description.contains("new.md"))
    }

    @Test
    fun `should describe SKIP action with reason`() {
        val pageNode = createMockPageNode("Page", "page.md")
        val action = SyncAction.skip(pageNode, "conf-123", "Unchanged")

        val description = action.describe()

        assertTrue(description.contains("[SKIP]"))
        assertTrue(description.contains("Unchanged"))
    }

    private fun createMockPageNode(title: String, path: String): PageNode {
        return PageNode(
            id = path,
            title = title,
            path = java.nio.file.Paths.get(path),
            document = null,
            isVirtual = false
        )
    }
}
