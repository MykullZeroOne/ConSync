package com.consync.service

import com.consync.config.*
import com.consync.core.converter.ConfluenceConverter
import com.consync.core.hierarchy.PageNode
import com.consync.core.markdown.MarkdownDocument
import com.consync.model.PageState
import com.consync.model.SyncActionType
import com.consync.model.SyncState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffServiceTest {

    private lateinit var config: Config
    private lateinit var converter: ConfluenceConverter
    private lateinit var diffService: DiffService

    @BeforeEach
    fun setup() {
        config = createTestConfig()
        converter = ConfluenceConverter(tocConfig = TocConfig(enabled = false))
        diffService = DiffService(config, converter)
    }

    @Test
    fun `should create action for new page`() {
        val rootNode = createRootNode(
            createPageNode("New Page", "new.md", "# New Page\n\nContent")
        )
        val state = SyncState.empty("DOCS")

        val plan = diffService.generatePlan(rootNode, state, rootPageId = "root-123")

        assertEquals(1, plan.createCount)
        assertEquals(0, plan.updateCount)
        val createAction = plan.createActions.first()
        assertEquals("New Page", createAction.title)
        assertEquals("new.md", createAction.relativePath)
    }

    @Test
    fun `should skip unchanged page`() {
        val content = "# Page\n\nContent"
        val convertedContent = converter.convert(content)
        val contentHash = hashContent(convertedContent)

        val rootNode = createRootNode(
            createPageNode("Page", "page.md", content)
        )
        val state = SyncState.empty("DOCS")
            .withPage("page.md", PageState.create(
                confluenceId = "conf-123",
                contentHash = contentHash,
                title = "Page",
                parentId = "root-123"
            ))

        val plan = diffService.generatePlan(rootNode, state, rootPageId = "root-123")

        assertEquals(0, plan.createCount)
        assertEquals(0, plan.updateCount)
        assertEquals(1, plan.skipCount)
    }

    @Test
    fun `should update page when content changed`() {
        val originalContent = "# Page\n\nOriginal"
        val newContent = "# Page\n\nUpdated content"

        val rootNode = createRootNode(
            createPageNode("Page", "page.md", newContent)
        )
        val state = SyncState.empty("DOCS")
            .withPage("page.md", PageState.create(
                confluenceId = "conf-123",
                contentHash = hashContent(converter.convert(originalContent)),
                title = "Page",
                parentId = "root-123"
            ))

        val plan = diffService.generatePlan(rootNode, state, rootPageId = "root-123")

        assertEquals(0, plan.createCount)
        assertEquals(1, plan.updateCount)
        assertEquals("Content changed", plan.updateActions.first().reason)
    }

    @Test
    fun `should update page when title changed`() {
        val content = "# New Title\n\nContent"
        val convertedContent = converter.convert(content)
        val contentHash = hashContent(convertedContent)

        val rootNode = createRootNode(
            createPageNode("New Title", "page.md", content)
        )
        val state = SyncState.empty("DOCS")
            .withPage("page.md", PageState.create(
                confluenceId = "conf-123",
                contentHash = contentHash,
                title = "Old Title",
                parentId = "root-123"
            ))

        val plan = diffService.generatePlan(rootNode, state, rootPageId = "root-123")

        assertEquals(1, plan.updateCount)
        assertEquals("Title changed", plan.updateActions.first().reason)
    }

    @Test
    fun `should force update all pages when force is true`() {
        val content = "# Page\n\nContent"
        val convertedContent = converter.convert(content)
        val contentHash = hashContent(convertedContent)

        val rootNode = createRootNode(
            createPageNode("Page", "page.md", content)
        )
        val state = SyncState.empty("DOCS")
            .withPage("page.md", PageState.create(
                confluenceId = "conf-123",
                contentHash = contentHash,
                title = "Page",
                parentId = "root-123"
            ))

        val plan = diffService.generatePlan(rootNode, state, rootPageId = "root-123", force = true)

        assertEquals(1, plan.updateCount)
        assertEquals("Force update", plan.updateActions.first().reason)
    }

    @Test
    fun `should delete orphaned pages when configured`() {
        val rootNode = createRootNode() // Empty root
        val state = SyncState.empty("DOCS")
            .withPage("orphan.md", PageState.create(
                confluenceId = "orphan-123",
                contentHash = "hash",
                title = "Orphan Page",
                parentId = null
            ))

        val plan = diffService.generatePlan(rootNode, state, rootPageId = null)

        assertEquals(1, plan.deleteCount)
        assertEquals("orphan-123", plan.deleteActions.first().confluenceId)
    }

    @Test
    fun `should not delete orphans when not configured`() {
        val noDeleteConfig = config.copy(sync = config.sync.copy(deleteOrphans = false))
        val diffServiceNoDelete = DiffService(noDeleteConfig, converter)

        val rootNode = createRootNode()
        val state = SyncState.empty("DOCS")
            .withPage("orphan.md", PageState.create(
                confluenceId = "orphan-123",
                contentHash = "hash",
                title = "Orphan",
                parentId = null
            ))

        val plan = diffServiceNoDelete.generatePlan(rootNode, state, rootPageId = null)

        assertEquals(0, plan.deleteCount)
    }

    @Test
    fun `should handle multiple pages`() {
        val content1 = "# Page 1"
        val content2 = "# Page 2"
        val content3 = "# Page 3 Updated"

        val rootNode = createRootNode(
            createPageNode("Page 1", "page1.md", content1),  // New
            createPageNode("Page 2", "page2.md", content2),  // Unchanged
            createPageNode("Page 3 Updated", "page3.md", content3)  // Updated title
        )

        val state = SyncState.empty("DOCS")
            .withPage("page2.md", PageState.create(
                confluenceId = "conf-2",
                contentHash = hashContent(converter.convert(content2)),
                title = "Page 2",
                parentId = "root-123"
            ))
            .withPage("page3.md", PageState.create(
                confluenceId = "conf-3",
                contentHash = hashContent(converter.convert(content3)),
                title = "Page 3",  // Old title
                parentId = "root-123"
            ))

        val plan = diffService.generatePlan(rootNode, state, rootPageId = "root-123")

        assertEquals(1, plan.createCount)
        assertEquals(1, plan.updateCount)
        assertEquals(1, plan.skipCount)
    }

    @Test
    fun `should sort actions with creates first`() {
        val rootNode = createRootNode(
            createPageNode("New", "new.md", "# New"),
            createPageNode("Existing", "existing.md", "# Existing Updated")
        )

        val state = SyncState.empty("DOCS")
            .withPage("existing.md", PageState.create(
                confluenceId = "conf-1",
                contentHash = "old-hash",
                title = "Existing",
                parentId = null
            ))

        val plan = diffService.generatePlan(rootNode, state, rootPageId = null)

        val modifyingActions = plan.actions.filter { it.type != SyncActionType.SKIP }
        assertEquals(SyncActionType.CREATE, modifyingActions.first().type)
    }

    private fun createTestConfig(): Config {
        return Config(
            confluence = ConfluenceConfig(
                baseUrl = "https://test.atlassian.net/wiki",
                username = "test",
                apiToken = "token"
            ),
            space = SpaceConfig(key = "DOCS"),
            content = ContentConfig(),
            sync = SyncConfig(deleteOrphans = true),
            files = FilesConfig(),
            logging = LoggingConfig()
        )
    }

    private fun createRootNode(vararg children: PageNode): PageNode {
        val root = PageNode(
            title = "Root",
            relativePath = "",
            document = null,
            isVirtual = true
        )
        children.forEach { root.addChild(it) }
        return root
    }

    private fun createPageNode(title: String, path: String, content: String): PageNode {
        val document = MarkdownDocument(
            relativePath = path,
            absolutePath = java.nio.file.Path.of("/test/$path"),
            content = content,
            frontmatter = null,
            title = title,
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            contentHash = ""
        )
        return PageNode(
            title = title,
            relativePath = path,
            document = document,
            isVirtual = false
        )
    }

    private fun hashContent(content: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return "sha256:" + hashBytes.joinToString("") { "%02x".format(it) }
    }
}
