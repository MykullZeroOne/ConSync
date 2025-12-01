package com.consync.integration

import com.consync.client.confluence.ConfluenceClient
import com.consync.client.confluence.model.*
import com.consync.config.Config
import com.consync.config.ConfigLoader
import com.consync.core.hierarchy.HierarchyBuilder
import com.consync.core.markdown.FrontmatterParser
import com.consync.core.markdown.MarkdownParser
import com.consync.model.SyncActionType
import com.consync.model.SyncState
import com.consync.service.DiffService
import com.consync.service.SyncStateManager
import com.consync.util.EnvLoader
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Full integration test that tests the entire sync workflow
 * with a real directory structure and mocked Confluence client.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullSyncIntegrationTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var contentDir: Path
    private lateinit var config: Config
    private lateinit var mockClient: ConfluenceClient

    @BeforeEach
    fun setup() {
        // Clear any previously loaded env vars
        EnvLoader.clear()

        // Create content directory structure
        contentDir = tempDir.resolve("docs")
        contentDir.createDirectories()

        // Create test directory structure
        createTestDocumentationStructure()

        // Create test .env file
        createTestEnvFile()

        // Create test config
        createTestConfig()

        // Load config with .env file
        val envPath = contentDir.resolve(".env")
        val configLoader = ConfigLoader.withEnvFile(envPath)
        config = configLoader.load(contentDir.resolve("consync.yaml"))

        // Create mock Confluence client
        mockClient = mockk(relaxed = true)
    }

    @AfterEach
    fun cleanup() {
        EnvLoader.clear()
        clearAllMocks()
    }

    // ==========================================
    // Directory Structure Creation
    // ==========================================

    private fun createTestDocumentationStructure() {
        // Root index.md
        contentDir.resolve("index.md").writeText("""
            ---
            title: Documentation Home
            weight: 1
            ---

            # Welcome

            This is the documentation home.

            ## Links

            - [Getting Started](./getting-started/index.md)
            - [Guides](./guides/index.md)
        """.trimIndent())

        // Getting Started section
        val gettingStarted = contentDir.resolve("getting-started")
        gettingStarted.createDirectories()

        gettingStarted.resolve("index.md").writeText("""
            ---
            title: Getting Started
            weight: 1
            ---

            # Getting Started

            Welcome to the getting started guide.

            - [Quick Start](./quickstart.md)
            - [Configuration](./configuration.md)
        """.trimIndent())

        gettingStarted.resolve("quickstart.md").writeText("""
            ---
            title: Quick Start
            weight: 2
            ---

            # Quick Start Guide

            Get started quickly!

            ```bash
            java -jar consync.jar sync
            ```
        """.trimIndent())

        gettingStarted.resolve("configuration.md").writeText("""
            ---
            title: Configuration
            weight: 3
            ---

            # Configuration

            Configure your installation.

            | Option | Description |
            |--------|-------------|
            | base_url | Confluence URL |
            | api_token | API token |
        """.trimIndent())

        // Guides section
        val guides = contentDir.resolve("guides")
        guides.createDirectories()

        guides.resolve("index.md").writeText("""
            ---
            title: User Guides
            weight: 2
            ---

            # User Guides

            Detailed guides for using the application.
        """.trimIndent())

        guides.resolve("advanced.md").writeText("""
            ---
            title: Advanced Usage
            weight: 1
            ---

            # Advanced Usage

            Advanced configuration options.

            - [ ] Task 1
            - [x] Task 2
            - [ ] Task 3
        """.trimIndent())
    }

    private fun createTestEnvFile() {
        contentDir.resolve(".env").writeText("""
            CONFLUENCE_BASE_URL=https://test.atlassian.net/wiki
            CONFLUENCE_USERNAME=test@example.com
            CONFLUENCE_API_TOKEN=test-api-token
            CONFLUENCE_SPACE_KEY=DOCS
            CONFLUENCE_ROOT_PAGE_ID=root-123
        """.trimIndent())
    }

    private fun createTestConfig() {
        contentDir.resolve("consync.yaml").writeText("""
            confluence:
              base_url: ${'$'}{CONFLUENCE_BASE_URL}
              username: ${'$'}{CONFLUENCE_USERNAME}
              api_token: ${'$'}{CONFLUENCE_API_TOKEN}

            space:
              key: ${'$'}{CONFLUENCE_SPACE_KEY}
              root_page_id: ${'$'}{CONFLUENCE_ROOT_PAGE_ID}

            content:
              title_source: frontmatter
              toc:
                enabled: true
                depth: 3

            sync:
              delete_orphans: true
              state_file: .consync/state.json

            files:
              include:
                - "**/*.md"
              exclude:
                - "**/node_modules/**"
              index_file: index.md

            logging:
              level: debug
        """.trimIndent())
    }

    // ==========================================
    // Tests
    // ==========================================

    @Test
    fun `should load configuration with env file expansion`() {
        assertEquals("https://test.atlassian.net/wiki", config.confluence.baseUrl)
        assertEquals("test@example.com", config.confluence.username)
        assertEquals("test-api-token", config.confluence.apiToken)
        assertEquals("DOCS", config.space.key)
        assertEquals("root-123", config.space.rootPageId)
    }

    @Test
    fun `should scan all markdown files`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )

        val files = scanner.scan(contentDir)

        assertEquals(6, files.size)
        assertTrue(files.any { it.fileName.toString() == "index.md" })
        assertTrue(files.any { it.toString().contains("quickstart.md") })
        assertTrue(files.any { it.toString().contains("advanced.md") })
    }

    @Test
    fun `should parse all markdown documents`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { file ->
            try {
                parser.parse(file, contentDir)
            } catch (e: Exception) {
                null
            }
        }

        assertEquals(6, documents.size)

        // Check frontmatter parsing
        val homeDoc = documents.find { it.title == "Documentation Home" }
        assertNotNull(homeDoc)
        assertEquals(1, homeDoc.frontmatter?.weight)

        // Check that content is preserved
        val quickstartDoc = documents.find { it.title == "Quick Start" }
        assertNotNull(quickstartDoc)
        assertTrue(quickstartDoc.content.contains("java -jar consync.jar sync"))
    }

    @Test
    fun `should build hierarchy from documents`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val hierarchyBuilder = HierarchyBuilder(indexFileName = config.files.indexFile)

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { parser.parse(it, contentDir) }
        val rootNode = hierarchyBuilder.build(documents, contentDir)

        // Root should have children
        assertTrue(rootNode.children.isNotEmpty())

        // Find the root index page
        val homeNode = rootNode.children.find { it.title == "Documentation Home" }
        assertNotNull(homeNode)

        // Find getting-started section
        val gettingStartedNode = rootNode.children.find { it.title == "Getting Started" }
        assertNotNull(gettingStartedNode)
        assertEquals(2, gettingStartedNode.children.size) // quickstart and configuration

        // Find guides section
        val guidesNode = rootNode.children.find { it.title == "User Guides" }
        assertNotNull(guidesNode)
        assertEquals(1, guidesNode.children.size) // advanced
    }

    @Test
    fun `should generate sync plan for new content`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val hierarchyBuilder = HierarchyBuilder(indexFileName = config.files.indexFile)
        val diffService = DiffService.create(config)

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { parser.parse(it, contentDir) }
        val rootNode = hierarchyBuilder.build(documents, contentDir)

        // Empty state - all pages are new
        val syncState = SyncState.empty(config.space.key, config.space.rootPageId)

        val plan = diffService.generatePlan(
            rootNode = rootNode,
            syncState = syncState,
            rootPageId = config.space.rootPageId
        )

        // All 6 pages should be CREATE actions
        assertEquals(6, plan.createCount)
        assertEquals(0, plan.updateCount)
        assertEquals(0, plan.deleteCount)
        assertEquals(0, plan.skipCount)

        // Verify creates are sorted (parents before children)
        val createPaths = plan.createActions.map { it.relativePath }
        val indexPos = createPaths.indexOf("index.md")
        val gettingStartedPos = createPaths.indexOf("getting-started/index.md")
        val quickstartPos = createPaths.indexOf("getting-started/quickstart.md")

        assertTrue(indexPos < quickstartPos || gettingStartedPos < quickstartPos,
            "Parent pages should come before children in CREATE order")
    }

    @Test
    fun `should generate update actions for changed content`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val hierarchyBuilder = HierarchyBuilder(indexFileName = config.files.indexFile)
        val diffService = DiffService.create(config)

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { parser.parse(it, contentDir) }
        val rootNode = hierarchyBuilder.build(documents, contentDir)

        // Create state with existing pages (but with different content hash)
        var syncState = SyncState.empty(config.space.key, config.space.rootPageId)
        syncState = syncState.withPage("index.md", com.consync.model.PageState.create(
            confluenceId = "page-1",
            contentHash = "sha256:old-hash-that-wont-match",
            title = "Documentation Home",
            parentId = config.space.rootPageId
        ))

        val plan = diffService.generatePlan(
            rootNode = rootNode,
            syncState = syncState,
            rootPageId = config.space.rootPageId
        )

        // index.md should be UPDATE, others should be CREATE
        assertEquals(5, plan.createCount)
        assertEquals(1, plan.updateCount)
        assertEquals(0, plan.deleteCount)

        val updateAction = plan.updateActions.first()
        assertEquals("index.md", updateAction.relativePath)
        assertEquals("page-1", updateAction.confluenceId)
    }

    @Test
    fun `should generate delete actions for orphaned pages`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val hierarchyBuilder = HierarchyBuilder(indexFileName = config.files.indexFile)
        val diffService = DiffService.create(config)

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { parser.parse(it, contentDir) }
        val rootNode = hierarchyBuilder.build(documents, contentDir)

        // Create state with an orphaned page (exists in state but not in files)
        var syncState = SyncState.empty(config.space.key, config.space.rootPageId)
        syncState = syncState.withPage("old-page.md", com.consync.model.PageState.create(
            confluenceId = "orphan-123",
            contentHash = "sha256:whatever",
            title = "Orphaned Page",
            parentId = config.space.rootPageId
        ))

        val plan = diffService.generatePlan(
            rootNode = rootNode,
            syncState = syncState,
            rootPageId = config.space.rootPageId
        )

        // Should have 1 DELETE for orphaned page
        assertEquals(1, plan.deleteCount)
        val deleteAction = plan.deleteActions.first()
        assertEquals("orphan-123", deleteAction.confluenceId)
        assertEquals("old-page.md", deleteAction.relativePath)
    }

    @Test
    fun `should persist and load sync state`() {
        val stateManager = SyncStateManager.forDirectory(contentDir)

        // Create initial state
        var state = SyncState.empty(config.space.key, config.space.rootPageId)
        state = state.withPage("index.md", com.consync.model.PageState.create(
            confluenceId = "page-1",
            contentHash = "sha256:abc123",
            title = "Home",
            parentId = config.space.rootPageId
        ))
        state = state.withPage("getting-started/index.md", com.consync.model.PageState.create(
            confluenceId = "page-2",
            contentHash = "sha256:def456",
            title = "Getting Started",
            parentId = "page-1"
        ))

        // Save state
        stateManager.save(state.withLastSync())

        // Load state
        val loadedState = stateManager.load(config.space.key, config.space.rootPageId)

        assertEquals(2, loadedState.pages.size)
        assertEquals("page-1", loadedState.getPage("index.md")?.confluenceId)
        assertEquals("page-2", loadedState.getPage("getting-started/index.md")?.confluenceId)
        assertNotNull(loadedState.lastSync)
    }

    @Test
    fun `should convert markdown to confluence storage format`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val converter = com.consync.core.converter.ConfluenceConverter(
            tocConfig = config.content.toc
        )

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { parser.parse(it, contentDir) }

        // Find the quickstart document which has a code block
        val quickstartDoc = documents.find { it.title == "Quick Start" }
        assertNotNull(quickstartDoc)

        val storageFormat = converter.convert(quickstartDoc.content)

        // Should contain TOC macro (enabled in config)
        assertTrue(storageFormat.contains("ac:structured-macro"))
        assertTrue(storageFormat.contains("ac:name=\"toc\""))

        // Should contain code macro for bash block
        assertTrue(storageFormat.contains("ac:name=\"code\""))
        assertTrue(storageFormat.contains("bash"))

        // Should contain the heading
        assertTrue(storageFormat.contains("<h1>Quick Start Guide</h1>"))
    }

    @Test
    fun `should handle task lists in conversion`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val converter = com.consync.core.converter.ConfluenceConverter(
            tocConfig = config.content.toc
        )

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { parser.parse(it, contentDir) }

        // Find the advanced document which has task lists
        val advancedDoc = documents.find { it.title == "Advanced Usage" }
        assertNotNull(advancedDoc)

        val storageFormat = converter.convert(advancedDoc.content)

        // Should contain task list elements
        assertTrue(storageFormat.contains("ac:task-list") || storageFormat.contains("<li>"))
    }

    @Test
    fun `should handle tables in conversion`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val converter = com.consync.core.converter.ConfluenceConverter(
            tocConfig = config.content.toc
        )

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { parser.parse(it, contentDir) }

        // Find the configuration document which has a table
        val configDoc = documents.find { it.title == "Configuration" }
        assertNotNull(configDoc)

        val storageFormat = converter.convert(configDoc.content)

        // Should contain table elements
        assertTrue(storageFormat.contains("<table"))
        assertTrue(storageFormat.contains("<th>"))
        assertTrue(storageFormat.contains("<td>"))
        assertTrue(storageFormat.contains("Option"))
        assertTrue(storageFormat.contains("Description"))
    }

    @Test
    fun `full workflow - scan, parse, build hierarchy, generate plan`() {
        // This test simulates the entire pre-sync workflow

        // 1. Scan files
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val files = scanner.scan(contentDir)
        assertEquals(6, files.size)

        // 2. Parse documents
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val documents = files.mapNotNull { parser.parse(it, contentDir) }
        assertEquals(6, documents.size)

        // 3. Build hierarchy
        val hierarchyBuilder = HierarchyBuilder(indexFileName = config.files.indexFile)
        val rootNode = hierarchyBuilder.build(documents, contentDir)
        assertTrue(rootNode.children.isNotEmpty())

        // 4. Load state
        val stateManager = SyncStateManager.forDirectory(contentDir)
        val syncState = stateManager.load(config.space.key, config.space.rootPageId)
        assertTrue(syncState.pages.isEmpty()) // First run

        // 5. Generate plan
        val diffService = DiffService.create(config)
        val plan = diffService.generatePlan(
            rootNode = rootNode,
            syncState = syncState,
            rootPageId = config.space.rootPageId
        )

        // Verify plan
        assertEquals(6, plan.createCount)
        assertEquals(0, plan.updateCount)
        assertTrue(plan.hasChanges)

        // Print plan summary for debugging
        println(plan.summary())
        println(plan.details())
    }

    @Test
    fun `should respect force sync flag`() {
        val scanner = com.consync.client.filesystem.FileScanner(
            includePatterns = config.files.include,
            excludePatterns = config.files.exclude
        )
        val parser = MarkdownParser(
            titleSource = config.content.titleSource,
            frontmatterParser = FrontmatterParser()
        )
        val hierarchyBuilder = HierarchyBuilder(indexFileName = config.files.indexFile)
        val diffService = DiffService.create(config)
        val converter = com.consync.core.converter.ConfluenceConverter(tocConfig = config.content.toc)

        val files = scanner.scan(contentDir)
        val documents = files.mapNotNull { parser.parse(it, contentDir) }
        val rootNode = hierarchyBuilder.build(documents, contentDir)

        // Create state with matching content hashes (normally would skip)
        var syncState = SyncState.empty(config.space.key, config.space.rootPageId)
        val indexDoc = documents.find { it.relativePath == "index.md" }!!
        val convertedContent = converter.convert(indexDoc.content)
        val hash = "sha256:" + java.security.MessageDigest.getInstance("SHA-256")
            .digest(convertedContent.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        syncState = syncState.withPage("index.md", com.consync.model.PageState.create(
            confluenceId = "page-1",
            contentHash = hash,
            title = "Documentation Home",
            parentId = config.space.rootPageId
        ))

        // Without force - should skip unchanged
        val normalPlan = diffService.generatePlan(
            rootNode = rootNode,
            syncState = syncState,
            rootPageId = config.space.rootPageId,
            force = false
        )
        assertEquals(1, normalPlan.skipCount)

        // With force - should update
        val forcePlan = diffService.generatePlan(
            rootNode = rootNode,
            syncState = syncState,
            rootPageId = config.space.rootPageId,
            force = true
        )
        assertEquals(0, forcePlan.skipCount)
        assertEquals(1, forcePlan.updateCount)
        assertEquals("Force update", forcePlan.updateActions.first().reason)
    }
}
