package com.consync.core.markdown

import org.commonmark.parser.Parser
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkExtractorTest {

    private val extractor = LinkExtractor()
    private val parser = Parser.builder().build()

    private fun parseMarkdown(content: String) = parser.parse(content)

    @Test
    fun `should extract internal markdown links`() {
        val content = """
            # Page

            See [another page](./other.md) for more info.
            Also check [guide](../guides/intro.md).
        """.trimIndent()

        val document = parseMarkdown(content)
        val links = extractor.extractLinks(document, Path.of("docs/page.md"))

        assertEquals(2, links.size)

        val link1 = links[0]
        assertEquals("another page", link1.text)
        assertEquals("./other.md", link1.href)
        assertTrue(link1.isInternal)
        assertFalse(link1.isExternal)
        assertFalse(link1.isAnchor)
        assertEquals(Path.of("docs/other.md"), link1.resolvedPath)

        val link2 = links[1]
        assertEquals("guide", link2.text)
        assertEquals("../guides/intro.md", link2.href)
        assertEquals(Path.of("guides/intro.md"), link2.resolvedPath)
    }

    @Test
    fun `should extract external links`() {
        val content = """
            Visit [Google](https://google.com) or [HTTP site](http://example.com).
        """.trimIndent()

        val document = parseMarkdown(content)
        val links = extractor.extractLinks(document, Path.of("page.md"))

        assertEquals(2, links.size)

        assertTrue(links[0].isExternal)
        assertFalse(links[0].isInternal)
        assertNull(links[0].resolvedPath)

        assertTrue(links[1].isExternal)
    }

    @Test
    fun `should extract anchor links`() {
        val content = """
            Jump to [section](#my-section) below.
        """.trimIndent()

        val document = parseMarkdown(content)
        val links = extractor.extractLinks(document, Path.of("page.md"))

        assertEquals(1, links.size)
        assertTrue(links[0].isAnchor)
        assertFalse(links[0].isInternal)
        assertNull(links[0].resolvedPath)
    }

    @Test
    fun `should handle links with anchors`() {
        val content = """
            See [section](./other.md#section-1).
        """.trimIndent()

        val document = parseMarkdown(content)
        val links = extractor.extractLinks(document, Path.of("page.md"))

        assertEquals(1, links.size)
        assertEquals("./other.md#section-1", links[0].href)
        assertTrue(links[0].isInternal)
        assertEquals(Path.of("other.md"), links[0].resolvedPath)
    }

    @Test
    fun `should extract images`() {
        val content = """
            # Page

            ![Alt text](./images/diagram.png)

            ![Remote](https://example.com/image.jpg)
        """.trimIndent()

        val document = parseMarkdown(content)
        val images = extractor.extractImages(document, Path.of("docs/page.md"))

        assertEquals(2, images.size)

        val img1 = images[0]
        assertEquals("Alt text", img1.altText)
        assertEquals("./images/diagram.png", img1.src)
        assertTrue(img1.isLocal)
        assertEquals(Path.of("docs/images/diagram.png"), img1.resolvedPath)

        val img2 = images[1]
        assertEquals("Remote", img2.altText)
        assertFalse(img2.isLocal)
        assertNull(img2.resolvedPath)
    }

    @Test
    fun `should resolve relative paths correctly`() {
        val testCases = listOf(
            Triple("./sibling.md", "docs/page.md", "docs/sibling.md"),
            Triple("../parent.md", "docs/sub/page.md", "docs/parent.md"),
            Triple("child/page.md", "docs/page.md", "docs/child/page.md"),
            Triple("./sub/deep/file.md", "page.md", "sub/deep/file.md")
        )

        for ((href, docPath, expected) in testCases) {
            val (resolved, isInternal) = extractor.resolveLink(href, Path.of(docPath))

            assertTrue(isInternal, "Expected internal for: $href from $docPath")
            assertEquals(Path.of(expected), resolved, "For: $href from $docPath")
        }
    }

    @Test
    fun `should not resolve non-markdown links`() {
        val nonMarkdownLinks = listOf(
            "image.png",
            "document.pdf",
            "styles.css",
            "script.js"
        )

        for (href in nonMarkdownLinks) {
            val (resolved, isInternal) = extractor.resolveLink(href, Path.of("page.md"))
            assertFalse(isInternal, "Should not be internal: $href")
        }
    }

    @Test
    fun `should handle mailto links`() {
        val content = """
            Contact [me](mailto:test@example.com).
        """.trimIndent()

        val document = parseMarkdown(content)
        val links = extractor.extractLinks(document, Path.of("page.md"))

        assertEquals(1, links.size)
        assertFalse(links[0].isInternal)
        assertFalse(links[0].isExternal)
        assertNull(links[0].resolvedPath)
    }

    @Test
    fun `should build link graph from documents`() {
        val doc1 = createMockDocument(
            "index.md",
            listOf(DocumentLink("A", "./a.md", Path.of("a.md"), true, false, 1))
        )
        val doc2 = createMockDocument(
            "a.md",
            listOf(
                DocumentLink("B", "./b.md", Path.of("b.md"), true, false, 1),
                DocumentLink("Index", "./index.md", Path.of("index.md"), true, false, 2)
            )
        )
        val doc3 = createMockDocument("b.md", emptyList())

        val graph = extractor.buildLinkGraph(listOf(doc1, doc2, doc3))

        assertEquals(2, graph.size)
        assertEquals(listOf(Path.of("a.md")), graph[Path.of("index.md")])
        assertEquals(listOf(Path.of("b.md"), Path.of("index.md")), graph[Path.of("a.md")])
        assertNull(graph[Path.of("b.md")])
    }

    @Test
    fun `should handle links with formatted text`() {
        val content = """
            See [**bold link**](./page.md) and [`code link`](./other.md).
        """.trimIndent()

        val document = parseMarkdown(content)
        val links = extractor.extractLinks(document, Path.of("index.md"))

        assertEquals(2, links.size)
        assertEquals("bold link", links[0].text)
        assertEquals("code link", links[1].text)
    }

    @Test
    fun `should handle image with title`() {
        val content = """
            ![Alt](./img.png "Image Title")
        """.trimIndent()

        val document = parseMarkdown(content)
        val images = extractor.extractImages(document, Path.of("page.md"))

        assertEquals(1, images.size)
        assertEquals("Image Title", images[0].title)
    }

    private fun createMockDocument(path: String, links: List<DocumentLink>): MarkdownDocument {
        return MarkdownDocument(
            relativePath = Path.of(path),
            absolutePath = Path.of("/root/$path"),
            rawContent = "",
            content = "",
            frontmatter = Frontmatter.EMPTY,
            title = path,
            links = links,
            images = emptyList(),
            headings = emptyList(),
            lastModified = java.time.Instant.now(),
            contentHash = "hash"
        )
    }
}
