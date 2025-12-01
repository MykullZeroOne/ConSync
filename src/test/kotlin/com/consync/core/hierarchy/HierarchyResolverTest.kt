package com.consync.core.hierarchy

import com.consync.core.markdown.DocumentLink
import com.consync.core.markdown.Frontmatter
import com.consync.core.markdown.MarkdownDocument
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant
import kotlin.test.*

class HierarchyResolverTest {

    private val resolver = HierarchyResolver()

    private fun createDoc(
        path: String,
        title: String? = null,
        links: List<DocumentLink> = emptyList()
    ): MarkdownDocument {
        val relativePath = Path.of(path)
        val computedTitle = title ?: path.substringAfterLast("/").removeSuffix(".md")

        return MarkdownDocument(
            relativePath = relativePath,
            absolutePath = Path.of("/root/$path"),
            rawContent = "# $computedTitle",
            content = "# $computedTitle",
            frontmatter = Frontmatter.EMPTY,
            title = computedTitle,
            links = links,
            images = emptyList(),
            headings = emptyList(),
            lastModified = Instant.now(),
            contentHash = "hash"
        )
    }

    private fun createLink(href: String, resolvedPath: String?): DocumentLink {
        return DocumentLink(
            text = "link",
            href = href,
            resolvedPath = resolvedPath?.let { Path.of(it) },
            isInternal = resolvedPath != null,
            isAnchor = href.startsWith("#"),
            lineNumber = 1
        )
    }

    private fun buildHierarchy(docs: List<MarkdownDocument>): HierarchyResult {
        return HierarchyBuilder().build(docs)
    }

    @Test
    fun `should resolve internal links`() {
        val docs = listOf(
            createDoc("page1.md", links = listOf(
                createLink("./page2.md", "page2.md")
            )),
            createDoc("page2.md")
        )

        val hierarchy = buildHierarchy(docs)
        val links = resolver.resolveLinks(hierarchy)

        assertEquals(1, links.size)
        assertEquals("page1.md", links[0].source.id)
        assertEquals("page2.md", links[0].target.id)
    }

    @Test
    fun `should find common ancestor`() {
        val docs = listOf(
            createDoc("docs/a/page1.md"),
            createDoc("docs/a/page2.md"),
            createDoc("docs/b/page3.md")
        )

        val hierarchy = buildHierarchy(docs)

        val page1 = hierarchy.getByPath(Path.of("docs/a/page1.md"))!!
        val page2 = hierarchy.getByPath(Path.of("docs/a/page2.md"))!!
        val page3 = hierarchy.getByPath(Path.of("docs/b/page3.md"))!!

        val common12 = resolver.findCommonAncestor(page1, page2)
        assertEquals("docs/a", common12?.id)

        val common13 = resolver.findCommonAncestor(page1, page3)
        assertEquals("docs", common13?.id)
    }

    @Test
    fun `should compute relative paths`() {
        val docs = listOf(
            createDoc("docs/intro.md"),
            createDoc("docs/guide.md"),
            createDoc("docs/advanced/tips.md")
        )

        val hierarchy = buildHierarchy(docs)

        val intro = hierarchy.getByPath(Path.of("docs/intro.md"))!!
        val guide = hierarchy.getByPath(Path.of("docs/guide.md"))!!
        val tips = hierarchy.getByPath(Path.of("docs/advanced/tips.md"))!!

        // Sibling
        val siblingPath = resolver.computeRelativePath(intro, guide)
        assertTrue(siblingPath.isNotEmpty())

        // To child directory
        val toChildPath = resolver.computeRelativePath(intro, tips)
        assertTrue(toChildPath.contains("advanced"))
    }

    @Test
    fun `should validate hierarchy`() {
        val docs = listOf(
            createDoc("page1.md"),
            createDoc("page2.md")
        )

        val hierarchy = buildHierarchy(docs)
        val errors = resolver.validate(hierarchy)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should get nodes at depth`() {
        val docs = listOf(
            createDoc("level1.md"),
            createDoc("dir/level2.md")
        )

        val hierarchy = buildHierarchy(docs)

        val depth0 = resolver.getNodesAtDepth(hierarchy, 0)
        assertEquals(1, depth0.size)
        assertTrue(depth0[0].isRoot)

        val depth1 = resolver.getNodesAtDepth(hierarchy, 1)
        assertEquals(2, depth1.size) // level1.md + dir (virtual)
    }

    @Test
    fun `should get leaf nodes`() {
        val docs = listOf(
            createDoc("dir/page1.md"),
            createDoc("dir/page2.md")
        )

        val hierarchy = buildHierarchy(docs)
        val leaves = resolver.getLeafNodes(hierarchy)

        assertEquals(2, leaves.size)
        assertTrue(leaves.all { it.isLeaf })
    }

    @Test
    fun `should get branch nodes`() {
        val docs = listOf(
            createDoc("dir/page.md")
        )

        val hierarchy = buildHierarchy(docs)
        val branches = resolver.getBranchNodes(hierarchy)

        // root and dir (virtual)
        assertEquals(2, branches.size)
        assertTrue(branches.all { !it.isLeaf })
    }

    @Test
    fun `should find broken links`() {
        val docs = listOf(
            createDoc("page.md", links = listOf(
                createLink("./exists.md", "exists.md"),
                createLink("./missing.md", "missing.md")
            )),
            createDoc("exists.md")
        )

        val hierarchy = buildHierarchy(docs)
        val broken = resolver.findBrokenLinks(hierarchy)

        assertEquals(1, broken.size)
        assertEquals("./missing.md", broken[0].href)
    }

    @Test
    fun `should build backlinks`() {
        val docs = listOf(
            createDoc("a.md", links = listOf(createLink("./b.md", "b.md"))),
            createDoc("b.md", links = listOf(createLink("./c.md", "c.md"))),
            createDoc("c.md", links = listOf(createLink("./b.md", "b.md")))
        )

        val hierarchy = buildHierarchy(docs)
        val backlinks = resolver.buildBacklinks(hierarchy)

        val bBacklinks = backlinks[hierarchy.getByPath(Path.of("b.md"))]
        assertNotNull(bBacklinks)
        assertEquals(2, bBacklinks.size) // a.md and c.md link to b.md
    }

    @Test
    fun `should calculate statistics`() {
        val docs = listOf(
            createDoc("index.md"),
            createDoc("docs/page1.md"),
            createDoc("docs/page2.md")
        )

        val hierarchy = buildHierarchy(docs)
        val stats = resolver.getStatistics(hierarchy)

        assertEquals(4, stats.totalNodes)  // root + docs(virtual) + 2 pages
        assertEquals(3, stats.realNodes)
        assertEquals(1, stats.virtualNodes)
        assertEquals(2, stats.leafNodes)
        assertEquals(2, stats.branchNodes)
        assertEquals(2, stats.maxDepth)
        assertEquals(0, stats.orphanDocuments)
    }

    @Test
    fun `should handle empty hierarchy`() {
        val hierarchy = buildHierarchy(emptyList())

        val links = resolver.resolveLinks(hierarchy)
        assertTrue(links.isEmpty())

        val errors = resolver.validate(hierarchy)
        assertTrue(errors.isEmpty())

        val stats = resolver.getStatistics(hierarchy)
        assertEquals(1, stats.totalNodes) // Just root
    }

    @Test
    fun `should resolve links with and without md extension`() {
        val docs = listOf(
            createDoc("page1.md", links = listOf(
                createLink("./page2", "page2"),  // Without .md
                createLink("./page3.md", "page3.md")  // With .md
            )),
            createDoc("page2.md"),
            createDoc("page3.md")
        )

        val hierarchy = buildHierarchy(docs)
        val links = resolver.resolveLinks(hierarchy)

        // Both should resolve
        assertEquals(2, links.size)
    }
}
