package com.consync.service

import com.consync.client.confluence.ConfluenceClient
import com.consync.client.confluence.model.*
import com.consync.client.confluence.exception.ConfluenceException
import com.consync.config.*
import com.consync.core.converter.ConfluenceConverter
import com.consync.core.hierarchy.PageNode
import com.consync.core.markdown.MarkdownDocument
import com.consync.core.markdown.Frontmatter
import com.consync.model.*
import java.nio.file.Paths
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SyncExecutorEndToEndTest {

    private lateinit var config: Config
    private lateinit var client: ConfluenceClient
    private lateinit var converter: ConfluenceConverter
    private lateinit var stateManager: SyncStateManager
    private lateinit var executor: SyncExecutor

    @BeforeEach
    fun setup() {
        config = createTestConfig()
        client = mockk()
        converter = ConfluenceConverter(tocConfig = TocConfig(enabled = false))
        stateManager = mockk()

        executor = SyncExecutor(config, client, converter, stateManager)
    }

    @Test
    fun `should_execute_create_action_successfully`() = runBlocking {
        val document = MarkdownDocument(
            relativePath = Paths.get("test.md"),
            absolutePath = Paths.get("/test/test.md"),
            rawContent = "# Test\n\nContent",
            content = "# Test\n\nContent",
            frontmatter = Frontmatter.EMPTY,
            title = "Test",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash"
        )

        val pageNode = PageNode(
            id = "test.md",
            title = "Test",
            path = Paths.get("test.md"),
            document = document,
            isVirtual = false
        )

        val action = SyncAction.create(
            pageNode = pageNode,
            parentId = "root-123",
            contentHash = "hash"
        )

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root-123",
            totalLocalPages = 1,
            totalConfluencePages = 0
        )

        val state = SyncState.empty("DOCS")

        coEvery { stateManager.save(any()) } returns Unit

        val createdPage = Page(
            id = "page-123",
            title = "Test",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.createPage(any()) } returns createdPage

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertEquals(1, result.createdCount)
        assertEquals(0, result.updatedCount)
        assertEquals(0, result.deletedCount)

        coVerify { client.createPage(any()) }
        coVerify { stateManager.save(any()) }
    }

    @Test
    fun `should_execute_update_action_successfully`() = runBlocking {
        val content = "# Test\n\nUpdated"
        val document = MarkdownDocument(
            relativePath = Paths.get("test.md"),
            absolutePath = Paths.get("/test/test.md"),
            rawContent = content,
            content = content,
            frontmatter = Frontmatter.EMPTY,
            title = "Test Updated",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "new-hash"
        )

        val pageNode = PageNode(
            id = "test.md",
            title = "Test Updated",
            path = Paths.get("test.md"),
            document = document,
            isVirtual = false
        )

        val action = SyncAction.update(
            pageNode = pageNode,
            confluenceId = "page-123",
            parentId = "root-123",
            contentHash = "new-hash",
            reason = "content changed"
        )

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root-123",
            totalLocalPages = 1,
            totalConfluencePages = 1
        )

        val state = SyncState.empty("DOCS")
            .withPage(
                "test.md",
                PageState.create(
                    confluenceId = "page-123",
                    contentHash = "old-hash",
                    title = "Test",
                    parentId = "root-123"
                )
            )

        coEvery { stateManager.save(any()) } returns Unit

        val currentPage = Page(
            id = "page-123",
            title = "Test",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.getPage("page-123", listOf("version")) } returns currentPage

        val updatedPage = currentPage.copy(version = Version(number = 2))
        coEvery { client.updatePage("page-123", any()) } returns updatedPage

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertEquals(0, result.createdCount)
        assertEquals(1, result.updatedCount)

        coVerify { client.updatePage("page-123", any()) }
        coVerify { stateManager.save(any()) }
    }

    @Test
    fun `should_execute_delete_action_successfully`() = runBlocking {
        val action = SyncAction.delete(
            confluenceId = "page-456",
            title = "Orphaned",
            relativePath = "orphaned.md"
        )

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root-123",
            totalLocalPages = 0,
            totalConfluencePages = 1
        )

        val state = SyncState.empty("DOCS")
            .withPage(
                "orphaned.md",
                PageState.create(
                    confluenceId = "page-456",
                    contentHash = "hash",
                    title = "Orphaned",
                    parentId = null
                )
            )

        coEvery { stateManager.save(any()) } returns Unit
        coEvery { client.deletePage("page-456") } returns Unit

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertEquals(1, result.deletedCount)

        coVerify { client.deletePage("page-456") }
        coVerify { stateManager.save(any()) }
    }

    @Test
    fun `should_execute_move_action_successfully`() = runBlocking {
        val document = MarkdownDocument(
            relativePath = Paths.get("guides/test.md"),
            absolutePath = Paths.get("/test/guides/test.md"),
            rawContent = "# Test",
            content = "# Test",
            frontmatter = Frontmatter.EMPTY,
            title = "Test",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash"
        )

        val parentNode = PageNode(
            id = "guides",
            title = "Guides",
            path = Paths.get("guides"),
            document = null,
            isVirtual = false
        )

        val pageNode = PageNode(
            id = "guides/test.md",
            title = "Test",
            path = Paths.get("guides/test.md"),
            document = document,
            isVirtual = false
        )
        pageNode.parent = parentNode

        val action = SyncAction.move(
            pageNode = pageNode,
            confluenceId = "page-789",
            newParentId = "guides-456",
            previousParentId = "root-123"
        )

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root-123",
            totalLocalPages = 1,
            totalConfluencePages = 1
        )

        val state = SyncState.empty("DOCS")
            .withPage(
                "guides/test.md",
                PageState.create(
                    confluenceId = "page-789",
                    contentHash = "hash",
                    title = "Test",
                    parentId = "root-123"
                )
            )

        coEvery { stateManager.save(any()) } returns Unit

        val movedPage = Page(
            id = "page-789",
            title = "Test",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.movePage("page-789", "guides-456") } returns movedPage

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertEquals(1, result.movedCount)

        coVerify { client.movePage("page-789", "guides-456") }
        coVerify { stateManager.save(any()) }
    }

    @Test
    fun `should_skip_unchanged_actions`() = runBlocking {
        val document = MarkdownDocument(
            relativePath = Paths.get("unchanged.md"),
            absolutePath = Paths.get("/test/unchanged.md"),
            rawContent = "# Unchanged",
            content = "# Unchanged",
            frontmatter = Frontmatter.EMPTY,
            title = "Unchanged",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash"
        )

        val pageNode = PageNode(
            id = "unchanged.md",
            title = "Unchanged",
            path = Paths.get("unchanged.md"),
            document = document,
            isVirtual = false
        )

        val action = SyncAction.skip(
            pageNode = pageNode,
            confluenceId = "page-999",
            reason = "no changes"
        )

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root-123",
            totalLocalPages = 1,
            totalConfluencePages = 1
        )

        val state = SyncState.empty("DOCS")

        coEvery { stateManager.save(any()) } returns Unit

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertEquals(0, result.createdCount)
        assertEquals(0, result.updatedCount)
        coVerify(exactly = 0) { client.createPage(any()) }
    }

    @Test
    fun `should_execute_multiple_actions_in_order`() = runBlocking {
        val createDoc = MarkdownDocument(
            relativePath = Paths.get("new.md"),
            absolutePath = Paths.get("/test/new.md"),
            rawContent = "# New",
            content = "# New",
            frontmatter = Frontmatter.EMPTY,
            title = "New",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash1"
        )

        val createNode = PageNode(
            id = "new.md",
            title = "New",
            path = Paths.get("new.md"),
            document = createDoc,
            isVirtual = false
        )

        val updateDoc = MarkdownDocument(
            relativePath = Paths.get("existing.md"),
            absolutePath = Paths.get("/test/existing.md"),
            rawContent = "# Existing Updated",
            content = "# Existing Updated",
            frontmatter = Frontmatter.EMPTY,
            title = "Existing Updated",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash2"
        )

        val updateNode = PageNode(
            id = "existing.md",
            title = "Existing Updated",
            path = Paths.get("existing.md"),
            document = updateDoc,
            isVirtual = false
        )

        val actions = listOf(
            SyncAction.create(pageNode = createNode, parentId = "root", contentHash = "hash1"),
            SyncAction.update(pageNode = updateNode, confluenceId = "page-1", parentId = "root", contentHash = "hash2", reason = "updated"),
            SyncAction.delete(confluenceId = "page-2", title = "Orphaned", relativePath = "orphaned.md")
        )

        val plan = SyncPlan(
            actions = actions,
            spaceKey = "DOCS",
            rootPageId = "root",
            totalLocalPages = 2,
            totalConfluencePages = 3
        )
        val state = SyncState.empty("DOCS")

        coEvery { stateManager.save(any()) } returns Unit

        coEvery { client.createPage(any()) } returns Page(
            id = "page-new",
            title = "New",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )

        coEvery { client.getPage("page-1", any()) } returns Page(
            id = "page-1",
            title = "Existing",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )

        coEvery { client.updatePage("page-1", any()) } returns Page(
            id = "page-1",
            title = "Existing Updated",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 2),
            links = PageLinks(webui = "http://example.com")
        )

        coEvery { client.deletePage("page-2") } returns Unit

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertEquals(1, result.createdCount)
        assertEquals(1, result.updatedCount)
        assertEquals(1, result.deletedCount)

        coVerify { client.createPage(any()) }
        coVerify { client.updatePage("page-1", any()) }
        coVerify { client.deletePage("page-2") }
    }

    @Test
    fun `should_handle_create_failure_gracefully`() = runBlocking {
        val document = MarkdownDocument(
            relativePath = Paths.get("test.md"),
            absolutePath = Paths.get("/test/test.md"),
            rawContent = "# Test",
            content = "# Test",
            frontmatter = Frontmatter.EMPTY,
            title = "Test",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash"
        )

        val pageNode = PageNode(
            id = "test.md",
            title = "Test",
            path = Paths.get("test.md"),
            document = document,
            isVirtual = false
        )

        val action = SyncAction.create(pageNode = pageNode, parentId = "root", contentHash = "hash")

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root",
            totalLocalPages = 1,
            totalConfluencePages = 0
        )
        val state = SyncState.empty("DOCS")

        coEvery { stateManager.save(any()) } returns Unit
        coEvery { client.createPage(any()) } throws ConfluenceException("API Error")

        val result = executor.execute(plan, dryRun = false, state)

        assertFalse(result.success)
        assertEquals(1, result.failedCount)
        assertEquals("API Error", result.errorMessage)

        coVerify { stateManager.save(any()) }
    }

    @Test
    fun `should_not_execute_on_dry_run`() = runBlocking {
        val document = MarkdownDocument(
            relativePath = Paths.get("test.md"),
            absolutePath = Paths.get("/test/test.md"),
            rawContent = "# Test",
            content = "# Test",
            frontmatter = Frontmatter.EMPTY,
            title = "Test",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash"
        )

        val pageNode = PageNode(
            id = "test.md",
            title = "Test",
            path = Paths.get("test.md"),
            document = document,
            isVirtual = false
        )

        val action = SyncAction.create(pageNode = pageNode, parentId = "root", contentHash = "hash")

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root",
            totalLocalPages = 1,
            totalConfluencePages = 0
        )
        val state = SyncState.empty("DOCS")

        val result = executor.execute(plan, dryRun = true, state)

        // Check that all actions are marked as dry-run
        assertTrue(result.actionResults.all { it.dryRun })
        coVerify(exactly = 0) { client.createPage(any()) }
        coVerify(exactly = 0) { stateManager.save(any()) }
    }

    @Test
    fun `should_handle_update_with_version_conflict`() = runBlocking {
        val document = MarkdownDocument(
            relativePath = Paths.get("test.md"),
            absolutePath = Paths.get("/test/test.md"),
            rawContent = "# Test Updated",
            content = "# Test Updated",
            frontmatter = Frontmatter.EMPTY,
            title = "Test",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "new-hash"
        )

        val pageNode = PageNode(
            id = "test.md",
            title = "Test",
            path = Paths.get("test.md"),
            document = document,
            isVirtual = false
        )

        val action = SyncAction.update(
            pageNode = pageNode,
            confluenceId = "page-1",
            parentId = "root",
            contentHash = "new-hash",
            reason = "updated"
        )

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root",
            totalLocalPages = 1,
            totalConfluencePages = 1
        )
        val state = SyncState.empty("DOCS")

        coEvery { stateManager.save(any()) } returns Unit

        coEvery { client.getPage("page-1", listOf("version")) } returns Page(
            id = "page-1",
            title = "Test",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 5),
            links = PageLinks(webui = "http://example.com")
        )

        coEvery { client.updatePage("page-1", any()) } returns Page(
            id = "page-1",
            title = "Test",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 6),
            links = PageLinks(webui = "http://example.com")
        )

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertEquals(1, result.updatedCount)

        coVerify { client.updatePage("page-1", match { it.version.number == 6 }) }
    }

    @Test
    fun `should_track_created_page_ids_for_parent_resolution`() = runBlocking {
        val parentDoc = MarkdownDocument(
            relativePath = Paths.get("guides/index.md"),
            absolutePath = Paths.get("/test/guides/index.md"),
            rawContent = "# Guides",
            content = "# Guides",
            frontmatter = Frontmatter.EMPTY,
            title = "Guides",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash1"
        )

        val parentNode = PageNode(
            id = "guides",
            title = "Guides",
            path = Paths.get("guides"),
            document = parentDoc,
            isVirtual = false
        )

        val childDoc = MarkdownDocument(
            relativePath = Paths.get("guides/getting-started.md"),
            absolutePath = Paths.get("/test/guides/getting-started.md"),
            rawContent = "# Getting Started",
            content = "# Getting Started",
            frontmatter = Frontmatter.EMPTY,
            title = "Getting Started",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash2"
        )

        val childNode = PageNode(
            id = "guides/getting-started.md",
            title = "Getting Started",
            path = Paths.get("guides/getting-started.md"),
            document = childDoc,
            isVirtual = false
        )
        childNode.parent = parentNode

        val createParent = SyncAction.create(
            pageNode = parentNode,
            parentId = "root",
            contentHash = "hash1"
        )

        val createChild = SyncAction.create(
            pageNode = childNode,
            parentId = "guides",
            contentHash = "hash2"
        )

        val plan = SyncPlan(
            actions = listOf(createParent, createChild),
            spaceKey = "DOCS",
            rootPageId = "root",
            totalLocalPages = 2,
            totalConfluencePages = 0
        )
        val state = SyncState.empty("DOCS")

        coEvery { stateManager.save(any()) } returns Unit

        var callCount = 0
        coEvery { client.createPage(any()) } answers {
            callCount++
            Page(
                id = "page-$callCount",
                title = "Page $callCount",
                space = SpaceReference(key = "DOCS"),
                version = Version(number = 1),
                links = PageLinks(webui = "http://example.com")
            )
        }

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertEquals(2, result.createdCount)

        coVerify(exactly = 2) { client.createPage(any()) }
    }

    @Test
    fun `should_save_final_state_with_last_sync_timestamp`() = runBlocking {
        val document = MarkdownDocument(
            relativePath = Paths.get("test.md"),
            absolutePath = Paths.get("/test/test.md"),
            rawContent = "# Test",
            content = "# Test",
            frontmatter = Frontmatter.EMPTY,
            title = "Test",
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash"
        )

        val pageNode = PageNode(
            id = "test.md",
            title = "Test",
            path = Paths.get("test.md"),
            document = document,
            isVirtual = false
        )

        val action = SyncAction.create(pageNode = pageNode, parentId = "root", contentHash = "hash")

        val plan = SyncPlan(
            actions = listOf(action),
            spaceKey = "DOCS",
            rootPageId = "root",
            totalLocalPages = 1,
            totalConfluencePages = 0
        )
        val state = SyncState.empty("DOCS")

        val savedStateSlot = slot<SyncState>()
        coEvery { stateManager.save(capture(savedStateSlot)) } returns Unit

        coEvery { client.createPage(any()) } returns Page(
            id = "page-123",
            title = "Test",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )

        val result = executor.execute(plan, dryRun = false, state)

        assertTrue(result.success)
        assertTrue(savedStateSlot.isCaptured)
        assertNotNull(savedStateSlot.captured.lastSync)
    }

    private fun createTestConfig(): Config {
        return Config(
            confluence = ConfluenceConfig(
                baseUrl = "https://test.atlassian.net/wiki",
                username = "test@example.com",
                apiToken = "test-token"
            ),
            space = SpaceConfig(
                key = "DOCS",
                name = "Documentation"
            ),
            content = ContentConfig(),
            sync = SyncConfig(deleteOrphans = true),
            files = FilesConfig(),
            logging = LoggingConfig()
        )
    }
}
