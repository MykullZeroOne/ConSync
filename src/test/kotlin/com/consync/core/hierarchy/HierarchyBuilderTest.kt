package com.consync.core.hierarchy

import com.consync.core.markdown.Frontmatter
import com.consync.core.markdown.MarkdownDocument
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant
import kotlin.test.*

class HierarchyBuilderTest {

    private fun createDoc(
        path: String,
        title: String? = null,
        weight: Int? = null,
        confluenceId: String? = null
    ): MarkdownDocument {
        val relativePath = Path.of(path)
        val fileName = relativePath.fileName.toString()
        val computedTitle = title ?: fileName.removeSuffix(".md")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        return MarkdownDocument(
            relativePath = relativePath,
            absolutePath = Path.of("/root/$path"),
            rawContent = "# $computedTitle",
            content = "# $computedTitle",
            frontmatter = Frontmatter(
                title = title,
                weight = weight,
                confluenceId = confluenceId
            ),
            title = computedTitle,
            links = emptyList(),
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash-$path"
        )
    }

    @Test
    fun `should build simple flat hierarchy`() {
        val docs = listOf(
            createDoc("page1.md"),
            createDoc("page2.md"),
            createDoc("page3.md")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        assertEquals(4, result.totalNodes) // root + 3 pages
        assertEquals(3, result.root.children.size)
    }

    @Test
    fun `should use index md for root`() {
        val docs = listOf(
            createDoc("index.md", title = "Home Page"),
            createDoc("about.md")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        assertEquals("Home Page", result.root.title)
        assertNotNull(result.root.document)
        assertEquals(1, result.root.children.size)
    }

    @Test
    fun `should build nested hierarchy`() {
        val docs = listOf(
            createDoc("index.md", title = "Home"),
            createDoc("docs/index.md", title = "Documentation"),
            createDoc("docs/guide.md", title = "Guide"),
            createDoc("docs/api.md", title = "API")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        assertEquals("Home", result.root.title)
        assertEquals(1, result.root.children.size) // docs directory

        val docsNode = result.root.children[0]
        assertEquals("Documentation", docsNode.title)
        assertEquals(2, docsNode.children.size) // guide + api
    }

    @Test
    fun `should create virtual nodes for directories without index`() {
        val docs = listOf(
            createDoc("guides/intro.md"),
            createDoc("guides/advanced.md")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        assertEquals(1, result.virtualNodes.size)
        val guidesNode = result.root.children[0]
        assertTrue(guidesNode.isVirtual)
        assertEquals("Guides", guidesNode.title)
        assertEquals(2, guidesNode.children.size)
    }

    @Test
    fun `should handle deep nesting`() {
        val docs = listOf(
            createDoc("a/b/c/d/deep.md")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        // Should create virtual nodes: a, a/b, a/b/c, a/b/c/d
        assertEquals(4, result.virtualNodes.size)
        assertEquals(5, result.maxDepth) // root(0) -> a(1) -> b(2) -> c(3) -> d(4) -> deep.md(5)

        val deepNode = result.getByPath(Path.of("a/b/c/d/deep.md"))
        assertNotNull(deepNode)
        assertEquals(5, deepNode.depth)
    }

    @Test
    fun `should sort by weight then title`() {
        val docs = listOf(
            createDoc("zebra.md", title = "Zebra", weight = 0),
            createDoc("alpha.md", title = "Alpha", weight = 0),
            createDoc("first.md", title = "First", weight = -1)
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        val titles = result.root.children.map { it.title }
        assertEquals(listOf("First", "Alpha", "Zebra"), titles)
    }

    @Test
    fun `should preserve confluence ID from frontmatter`() {
        val docs = listOf(
            createDoc("page.md", confluenceId = "12345")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        val node = result.root.children[0]
        assertEquals("12345", node.confluenceId)
    }

    @Test
    fun `should find nodes by path`() {
        val docs = listOf(
            createDoc("docs/guide.md")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        assertNotNull(result.getByPath(Path.of("docs/guide.md")))
        assertNotNull(result.getByPath(Path.of("docs")))
        assertNull(result.getByPath(Path.of("nonexistent")))
    }

    @Test
    fun `should find nodes by id`() {
        val docs = listOf(
            createDoc("my-page.md")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        assertNotNull(result.getById("my-page.md"))
        assertNull(result.getById("nonexistent"))
    }

    @Test
    fun `should handle mixed structure`() {
        val docs = listOf(
            createDoc("index.md", title = "Home"),
            createDoc("about.md"),
            createDoc("docs/index.md", title = "Docs"),
            createDoc("docs/intro.md"),
            createDoc("docs/advanced/index.md", title = "Advanced"),
            createDoc("docs/advanced/tips.md"),
            createDoc("blog/post1.md")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        assertEquals("Home", result.root.title)
        assertEquals(3, result.root.children.size) // about, docs, blog

        val docsNode = result.getByPath(Path.of("docs"))
        assertNotNull(docsNode)
        assertEquals(2, docsNode?.children?.size) // intro, advanced

        val advancedNode = result.getByPath(Path.of("docs/advanced"))
        assertNotNull(advancedNode)
        assertEquals("Advanced", advancedNode?.title)
    }

    @Test
    fun `should print tree structure`() {
        val docs = listOf(
            createDoc("index.md", title = "Root"),
            createDoc("docs/page.md", title = "Page")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        val tree = result.printTree()
        assertTrue(tree.contains("Root"))
        assertTrue(tree.contains("Docs"))  // Virtual node
        assertTrue(tree.contains("Page"))
    }

    @Test
    fun `should handle empty document list`() {
        val builder = HierarchyBuilder(rootTitle = "Empty Root")
        val result = builder.build(emptyList())

        assertEquals("Empty Root", result.root.title)
        assertTrue(result.root.children.isEmpty())
        assertEquals(1, result.totalNodes)
    }

    @Test
    fun `should handle custom root title`() {
        val builder = HierarchyBuilder(rootTitle = "My Docs")
        val result = builder.build(emptyList())

        assertEquals("My Docs", result.root.title)
    }

    @Test
    fun `should report statistics correctly`() {
        val docs = listOf(
            createDoc("index.md"),
            createDoc("docs/page.md"),
            createDoc("docs/guide.md")
        )

        val builder = HierarchyBuilder()
        val result = builder.build(docs)

        assertEquals(4, result.totalNodes)   // root + docs(virtual) + 2 pages
        assertEquals(3, result.realNodes)    // root + 2 pages
        assertEquals(1, result.virtualNodes.size) // docs directory
    }
}
