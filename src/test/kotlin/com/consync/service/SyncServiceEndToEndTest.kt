package com.consync.service

import com.consync.client.confluence.ConfluenceClient
import com.consync.client.confluence.model.*
import com.consync.client.confluence.exception.ConfluenceException
import com.consync.config.*
import com.consync.model.SyncActionType
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SyncServiceEndToEndTest {

    private lateinit var tempDir: Path
    private lateinit var config: Config
    private lateinit var client: ConfluenceClient
    private lateinit var stateManager: SyncStateManager
    private lateinit var syncService: SyncService

    @BeforeEach
    fun setup(@TempDir workDir: Path) {
        tempDir = workDir
        
        config = createTestConfig()
        client = mockk()
        stateManager = mockk()
        
        syncService = SyncService(
            config = config,
            contentDir = tempDir,
            client = client,
            stateManager = stateManager
        )
    }

    @Test
    fun `should_sync_new_markdown_file_from_start_to_finish`() = runBlocking {
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, "# New Document\n\nThis is a test document.".toByteArray())

        // Mock state manager
        every { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")
        every { stateManager.save(any()) } returns Unit
        every { stateManager.exists() } returns false

        // Mock Confluence client
        coEvery { client.getPageByTitle(any(), any()) } returns null
        val createdPage = Page(
            id = "page-123",
            title = "New Document",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.createPage(any()) } returns createdPage

        val result = syncService.sync(dryRun = false)

        assertTrue(result.success)
        assertEquals(1, result.createdCount)
        assertEquals(0, result.updatedCount)
        
        coVerify { client.createPage(any()) }
        verify { stateManager.save(any()) }
    }

    @Test
    fun `should_update_existing_page_when_content_changes`() = runBlocking {
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, "# Updated Document\n\nThis content has been updated.".toByteArray())

        val existingState = com.consync.model.SyncState.empty("DOCS")
            .withPage(
                "document.md",
                com.consync.model.PageState.create(
                    confluenceId = "page-123",
                    contentHash = "old-hash",
                    title = "Document",
                    parentId = null
                )
            )

        every { stateManager.load(any(), any()) } returns existingState
        every { stateManager.save(any()) } returns Unit
        every { stateManager.exists() } returns true

        val currentPage = Page(
            id = "page-123",
            title = "Document",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.getPage("page-123", listOf("version")) } returns currentPage

        val updatedPage = currentPage.copy(version = Version(number = 2))
        coEvery { client.updatePage("page-123", any()) } returns updatedPage

        val result = syncService.sync(dryRun = false)

        assertTrue(result.success)
        assertEquals(0, result.createdCount)
        assertEquals(1, result.updatedCount)
        
        coVerify { client.updatePage("page-123", any()) }
        verify { stateManager.save(any()) }
    }

    @Test
    fun `should_skip_unchanged_pages`() = runBlocking {
        val content = "# Document\n\nUnchanged content."
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, content.toByteArray())

        val contentHash = computeContentHash(content)
        val existingState = com.consync.model.SyncState.empty("DOCS")
            .withPage(
                "document.md",
                com.consync.model.PageState.create(
                    confluenceId = "page-123",
                    contentHash = contentHash,
                    title = "Document",
                    parentId = null
                )
            )

        every { stateManager.load(any(), any()) } returns existingState
        every { stateManager.save(any()) } returns Unit
        every { stateManager.exists() } returns true

        val result = syncService.sync(dryRun = false)

        assertTrue(result.success)
        assertEquals(0, result.createdCount)
        assertEquals(0, result.updatedCount)
        coVerify(exactly = 0) { client.createPage(any()) }
        coVerify(exactly = 0) { client.updatePage(any(), any()) }
    }

    @Test
    fun `should_perform_dry_run_without_making_changes`() = runBlocking {
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, "# New Document\n\nContent.".toByteArray())

        every { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")
        every { stateManager.save(any()) } returns Unit

        val result = syncService.sync(dryRun = true)

        // Check that all actions are marked as dry-run
        assertTrue(result.actionResults.all { it.dryRun })
        coVerify(exactly = 0) { client.createPage(any()) }
        coVerify(exactly = 0) { client.updatePage(any(), any()) }
        verify(exactly = 0) { stateManager.save(any()) }
    }

    @Test
    fun `should_handle_multiple_files_with_hierarchy`() = runBlocking {
        // Create directory structure
        Files.createDirectory(tempDir.resolve("guides"))
        
        Files.write(tempDir.resolve("index.md"), "# Home\n\nWelcome.".toByteArray())
        Files.write(tempDir.resolve("guides").resolve("index.md"), "# Guides\n\nGuide content.".toByteArray())
        Files.write(tempDir.resolve("guides").resolve("getting-started.md"), "# Getting Started\n\nStart here.".toByteArray())

        every { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")
        every { stateManager.save(any()) } returns Unit

        // Mock page creation
        var callCount = 0
        coEvery { client.createPage(any()) } answers {
            val id = callCount++
            Page(
                id = "page-$id",
                title = "Page $id",
                space = SpaceReference(key = "DOCS"),
                version = Version(number = 1),
                links = PageLinks(webui = "http://example.com")
            )
        }

        val result = syncService.sync(dryRun = false)

        assertTrue(result.success)
        assertEquals(3, result.createdCount)
    }

    @Test
    fun `should_delete_orphaned_pages_when_configured`() = runBlocking {
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, "# Document\n\nContent.".toByteArray())

        val existingState = com.consync.model.SyncState.empty("DOCS")
            .withPage(
                "document.md",
                com.consync.model.PageState.create(
                    confluenceId = "page-123",
                    contentHash = "hash",
                    title = "Document",
                    parentId = null
                )
            )
            .withPage(
                "orphaned.md",
                com.consync.model.PageState.create(
                    confluenceId = "orphan-456",
                    contentHash = "hash",
                    title = "Orphaned Page",
                    parentId = null
                )
            )

        coEvery { stateManager.load(any(), any()) } returns existingState
        coEvery { stateManager.save(any()) } returns Unit

        val currentPage = Page(
            id = "page-123",
            title = "Document",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.getPage("page-123", listOf("version")) } returns currentPage
        coEvery { client.updatePage("page-123", any()) } returns currentPage

        coEvery { client.deletePage("orphan-456") } returns Unit

        val configWithDelete = config.copy(sync = config.sync.copy(deleteOrphans = true))
        val serviceWithDelete = SyncService(configWithDelete, tempDir, client, stateManager)

        val result = serviceWithDelete.sync(dryRun = false)

        assertTrue(result.success)
        assertEquals(1, result.deletedCount)
        coVerify { client.deletePage("orphan-456") }
    }

    @Test
    fun `should_force_update_all_pages_when_flag_set`() = runBlocking {
        val content = "# Document\n\nContent."
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, content.toByteArray())

        val contentHash = computeContentHash(content)
        val existingState = com.consync.model.SyncState.empty("DOCS")
            .withPage(
                "document.md",
                com.consync.model.PageState.create(
                    confluenceId = "page-123",
                    contentHash = contentHash,
                    title = "Document",
                    parentId = null
                )
            )

        coEvery { stateManager.load(any(), any()) } returns existingState
        coEvery { stateManager.save(any()) } returns Unit

        val currentPage = Page(
            id = "page-123",
            title = "Document",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.getPage("page-123", listOf("version")) } returns currentPage

        val updatedPage = currentPage.copy(version = Version(number = 2))
        coEvery { client.updatePage("page-123", any()) } returns updatedPage

        val result = syncService.sync(dryRun = false, force = true)

        assertTrue(result.success)
        assertEquals(1, result.updatedCount)
        coVerify { client.updatePage("page-123", any()) }
    }

    @Test
    fun `should_report_status_correctly`() = runBlocking {
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, "# Document\n\nContent.".toByteArray())

        val state = com.consync.model.SyncState.empty("DOCS")
            .withPage(
                "document.md",
                com.consync.model.PageState.create(
                    confluenceId = "page-123",
                    contentHash = "hash",
                    title = "Document",
                    parentId = null
                )
            )

        every { stateManager.load(any(), any()) } returns state
        every { stateManager.exists() } returns true

        val status = syncService.status()

        assertEquals("DOCS", status.spaceKey)
        assertEquals(1, status.localFileCount)
        assertEquals(1, status.trackedPageCount)
        assertTrue(status.stateFileExists)
    }

    @Test
    fun `should_handle_sync_plan_generation_without_execution`() = runBlocking {
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, "# Document\n\nContent.".toByteArray())

        coEvery { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")

        val plan = syncService.plan(force = false)

        assertTrue(plan.hasChanges)
        assertEquals(1, plan.createCount)
    }

    @Test
    fun `should_handle_multiple_creates_and_updates_together`() = runBlocking {
        // Create files
        Files.write(tempDir.resolve("new.md"), "# New Document\n\nNew content.".toByteArray())
        Files.write(tempDir.resolve("existing.md"), "# Updated Existing\n\nUpdated content.".toByteArray())

        val newContentHash = computeContentHash("# Updated Existing\n\nUpdated content.")
        val oldContentHash = "old-hash"

        val existingState = com.consync.model.SyncState.empty("DOCS")
            .withPage(
                "existing.md",
                com.consync.model.PageState.create(
                    confluenceId = "page-123",
                    contentHash = oldContentHash,
                    title = "Existing",
                    parentId = null
                )
            )

        coEvery { stateManager.load(any(), any()) } returns existingState
        coEvery { stateManager.save(any()) } returns Unit

        val createdPage = Page(
            id = "page-new",
            title = "New Document",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.createPage(any()) } returns createdPage

        val existingPage = Page(
            id = "page-123",
            title = "Existing",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.getPage("page-123", any()) } returns existingPage

        val updatedPage = existingPage.copy(version = Version(number = 2))
        coEvery { client.updatePage("page-123", any()) } returns updatedPage

        val result = syncService.sync(dryRun = false)

        assertTrue(result.success)
        assertEquals(1, result.createdCount)
        assertEquals(1, result.updatedCount)
        coVerify { client.createPage(any()) }
        coVerify { client.updatePage("page-123", any()) }
    }

    @Test
    fun `should_handle_nested_directory_hierarchy`() = runBlocking {
        // Create nested structure
        Files.createDirectories(tempDir.resolve("docs/guides/advanced"))

        Files.write(tempDir.resolve("index.md"), "# Home".toByteArray())
        Files.write(tempDir.resolve("docs/index.md"), "# Documentation".toByteArray())
        Files.write(tempDir.resolve("docs/guides/index.md"), "# Guides".toByteArray())
        Files.write(tempDir.resolve("docs/guides/advanced/index.md"), "# Advanced".toByteArray())
        Files.write(tempDir.resolve("docs/guides/getting-started.md"), "# Getting Started".toByteArray())

        every { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")
        every { stateManager.save(any()) } returns Unit

        var pageCounter = 0
        coEvery { client.createPage(any()) } answers {
            Page(
                id = "page-${pageCounter++}",
                title = "Page",
                space = SpaceReference(key = "DOCS"),
                version = Version(number = 1),
                links = PageLinks(webui = "http://example.com")
            )
        }

        val result = syncService.sync(dryRun = false)

        assertTrue(result.success)
        // Note: Creates 6 pages because hierarchy builder creates nodes for both
        // directories and their index.md files (e.g., docs + docs/index.md)
        assertEquals(6, result.createdCount)
    }

    @Test
    fun `should_handle_sync_failure_and_partial_state_recovery`() = runBlocking {
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, "# Document\n\nContent.".toByteArray())

        every { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")
        every { stateManager.save(any()) } returns Unit

        coEvery { client.createPage(any()) } throws ConfluenceException("API Error")

        val result = syncService.sync(dryRun = false)

        assertFalse(result.success)
        verify { stateManager.save(any()) }
    }

    @Test
    fun `should_reset_sync_state`() = runBlocking {
        every { stateManager.reset() } returns Unit

        syncService.reset()

        verify { stateManager.reset() }
    }

    @Test
    fun `should_handle_empty_markdown_files_gracefully`() = runBlocking {
        val markdownFile = tempDir.resolve("empty.md")
        Files.write(markdownFile, "".toByteArray())

        every { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")
        every { stateManager.save(any()) } returns Unit

        coEvery { client.createPage(any()) } returns Page(
            id = "page-empty",
            title = "Empty",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )

        val result = syncService.sync(dryRun = false)

        // Empty file should be synced successfully (with title from filename)
        assertTrue(result.success)
        assertEquals(1, result.createdCount)
    }

    @Test
    fun `should_handle_markdown_with_special_characters`() = runBlocking {
        val content = """
            # Document with Special Characters
            
            Contains: **bold**, *italic*, `code`
            
            - List item 1
            - List item 2
            
            [Link](https://example.com)
        """.trimIndent()
        
        val markdownFile = tempDir.resolve("special.md")
        Files.write(markdownFile, content.toByteArray())

        every { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")
        every { stateManager.save(any()) } returns Unit

        val createdPage = Page(
            id = "page-123",
            title = "Document with Special Characters",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )
        coEvery { client.createPage(any()) } returns createdPage

        val result = syncService.sync(dryRun = false)

        assertTrue(result.success)
        assertEquals(1, result.createdCount)
    }

    @Test
    fun `should_handle_title_extraction_from_heading`() = runBlocking {
        val content = "# My Title\n\nSome content here."
        val markdownFile = tempDir.resolve("document.md")
        Files.write(markdownFile, content.toByteArray())

        every { stateManager.load(any(), any()) } returns com.consync.model.SyncState.empty("DOCS")
        every { stateManager.save(any()) } returns Unit

        val createdPage = Page(
            id = "page-123",
            title = "My Title",
            space = SpaceReference(key = "DOCS"),
            version = Version(number = 1),
            links = PageLinks(webui = "http://example.com")
        )

        val capturedRequestSlot = slot<CreatePageRequest>()
        coEvery { client.createPage(capture(capturedRequestSlot)) } returns createdPage

        // Create a custom service with FIRST_HEADING title source
        val customConfig = config.copy(
            content = config.content.copy(titleSource = TitleSource.FIRST_HEADING)
        )
        val customService = SyncService(customConfig, tempDir, client, stateManager)

        val result = customService.sync(dryRun = false)

        assertTrue(result.success)
        assertEquals("My Title", capturedRequestSlot.captured.title)
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
            sync = SyncConfig(deleteOrphans = false),
            files = FilesConfig(),
            logging = LoggingConfig()
        )
    }

    private fun computeContentHash(content: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
    }
}
