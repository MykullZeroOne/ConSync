package com.consync.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SyncStateTest {

    @Test
    fun `should create empty state`() {
        val state = SyncState.empty("DOCS", "12345")

        assertEquals("DOCS", state.spaceKey)
        assertEquals("12345", state.rootPageId)
        assertTrue(state.pages.isEmpty())
        assertNull(state.lastSync)
    }

    @Test
    fun `should add page to state`() {
        val state = SyncState.empty("DOCS")
        val pageState = PageState.create(
            confluenceId = "page-123",
            contentHash = "sha256:abc123",
            title = "Test Page",
            parentId = "root-123"
        )

        val updated = state.withPage("docs/test.md", pageState)

        assertEquals(1, updated.pages.size)
        assertEquals(pageState, updated.getPage("docs/test.md"))
    }

    @Test
    fun `should remove page from state`() {
        val pageState = PageState.create(
            confluenceId = "page-123",
            contentHash = "sha256:abc123",
            title = "Test Page",
            parentId = null
        )
        val state = SyncState.empty("DOCS").withPage("docs/test.md", pageState)

        val updated = state.withoutPage("docs/test.md")

        assertTrue(updated.pages.isEmpty())
        assertNull(updated.getPage("docs/test.md"))
    }

    @Test
    fun `should find page by confluence id`() {
        val pageState = PageState.create(
            confluenceId = "page-123",
            contentHash = "sha256:abc123",
            title = "Test Page",
            parentId = null
        )
        val state = SyncState.empty("DOCS").withPage("docs/test.md", pageState)

        val found = state.getPageById("page-123")

        assertEquals(pageState, found)
    }

    @Test
    fun `should detect content change`() {
        val pageState = PageState.create(
            confluenceId = "page-123",
            contentHash = "sha256:abc123",
            title = "Test Page",
            parentId = null
        )
        val state = SyncState.empty("DOCS").withPage("docs/test.md", pageState)

        assertTrue(state.hasContentChanged("docs/test.md", "sha256:different"))
        assertFalse(state.hasContentChanged("docs/test.md", "sha256:abc123"))
        assertTrue(state.hasContentChanged("docs/new.md", "sha256:abc123"))
    }

    @Test
    fun `should serialize and deserialize`() {
        val pageState = PageState.create(
            confluenceId = "page-123",
            contentHash = "sha256:abc123",
            title = "Test Page",
            parentId = "root-123"
        )
        val state = SyncState.empty("DOCS", "root-123")
            .withPage("docs/test.md", pageState)
            .withLastSync()

        val json = state.toJson()
        val deserialized = SyncState.fromJson(json)

        assertEquals(state.spaceKey, deserialized.spaceKey)
        assertEquals(state.rootPageId, deserialized.rootPageId)
        assertEquals(state.pages.size, deserialized.pages.size)
        assertEquals(state.getPage("docs/test.md")?.confluenceId, deserialized.getPage("docs/test.md")?.confluenceId)
    }

    @Test
    fun `should return tracked paths`() {
        val state = SyncState.empty("DOCS")
            .withPage("docs/a.md", PageState.create("1", "hash1", "A", null))
            .withPage("docs/b.md", PageState.create("2", "hash2", "B", null))

        val paths = state.trackedPaths()

        assertEquals(setOf("docs/a.md", "docs/b.md"), paths)
    }

    @Test
    fun `should return tracked ids`() {
        val state = SyncState.empty("DOCS")
            .withPage("docs/a.md", PageState.create("page-1", "hash1", "A", null))
            .withPage("docs/b.md", PageState.create("page-2", "hash2", "B", null))

        val ids = state.trackedIds()

        assertEquals(setOf("page-1", "page-2"), ids)
    }
}

class PageStateTest {

    @Test
    fun `should create page state`() {
        val pageState = PageState.create(
            confluenceId = "page-123",
            contentHash = "sha256:abc123",
            title = "Test Page",
            parentId = "parent-456"
        )

        assertEquals("page-123", pageState.confluenceId)
        assertEquals("sha256:abc123", pageState.contentHash)
        assertEquals("Test Page", pageState.title)
        assertEquals("parent-456", pageState.parentId)
        assertEquals(1, pageState.version)
    }

    @Test
    fun `should update page state`() {
        val original = PageState.create(
            confluenceId = "page-123",
            contentHash = "sha256:abc123",
            title = "Original Title",
            parentId = null
        )

        val updated = original.updated(
            contentHash = "sha256:def456",
            title = "New Title",
            version = 2
        )

        assertEquals("sha256:def456", updated.contentHash)
        assertEquals("New Title", updated.title)
        assertEquals(2, updated.version)
        assertEquals(original.confluenceId, updated.confluenceId)
    }
}
