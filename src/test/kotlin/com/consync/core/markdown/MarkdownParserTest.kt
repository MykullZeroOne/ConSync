package com.consync.core.markdown

import com.consync.config.TitleSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarkdownParserTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should parse simple markdown document`() {
        val parser = MarkdownParser()
        val content = """
            # Hello World

            This is a paragraph.

            ## Section 1

            More content here.
        """.trimIndent()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("test.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertEquals(Path.of("test.md"), doc.relativePath)
        assertEquals("Test", doc.title) // From filename
        assertEquals(content, doc.content)
        assertEquals(2, doc.headings.size)
        assertEquals("Hello World", doc.headings[0].text)
        assertEquals(1, doc.headings[0].level)
        assertEquals("Section 1", doc.headings[1].text)
        assertEquals(2, doc.headings[1].level)
    }

    @Test
    fun `should parse document with frontmatter`() {
        val parser = MarkdownParser(TitleSource.FRONTMATTER)
        val content = """
            ---
            title: My Custom Title
            description: A test document
            tags: [test, example]
            ---
            # Heading

            Content here.
        """.trimIndent()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("test.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertEquals("My Custom Title", doc.title)
        assertEquals("My Custom Title", doc.frontmatter.title)
        assertEquals("A test document", doc.frontmatter.description)
        assertEquals(listOf("test", "example"), doc.frontmatter.tags)
        assertFalse(doc.content.contains("---"))
    }

    @Test
    fun `should use first heading as title when configured`() {
        val parser = MarkdownParser(TitleSource.FIRST_HEADING)
        val content = """
            # Document Title From Heading

            Some content.
        """.trimIndent()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("test.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertEquals("Document Title From Heading", doc.title)
    }

    @Test
    fun `should use filename as title when configured`() {
        val parser = MarkdownParser(TitleSource.FILENAME)
        val content = """
            ---
            title: Frontmatter Title
            ---
            # Heading Title

            Content.
        """.trimIndent()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("my-page-name.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertEquals("My Page Name", doc.title)
    }

    @Test
    fun `should use parent directory name for index files`() {
        val parser = MarkdownParser(TitleSource.FILENAME)
        val content = "# Index content"

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("getting-started/index.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertEquals("Getting Started", doc.title)
        assertTrue(doc.isIndex)
    }

    @Test
    fun `should extract links from document`() {
        val parser = MarkdownParser()
        val content = """
            # Links

            See [other page](./other.md) and [external](https://example.com).
        """.trimIndent()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("docs/page.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertEquals(2, doc.links.size)
        assertTrue(doc.links[0].isInternal)
        assertTrue(doc.links[1].isExternal)
    }

    @Test
    fun `should extract images from document`() {
        val parser = MarkdownParser()
        val content = """
            # Images

            ![Local](./images/photo.png)
            ![Remote](https://example.com/img.jpg)
        """.trimIndent()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("page.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertEquals(2, doc.images.size)
        assertTrue(doc.images[0].isLocal)
        assertFalse(doc.images[1].isLocal)
    }

    @Test
    fun `should calculate content hash`() {
        val parser = MarkdownParser()
        val content = "# Test content"

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("test.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertNotNull(doc.contentHash)
        assertTrue(doc.contentHash.length == 64) // SHA-256 hex length
    }

    @Test
    fun `should generate anchor IDs for headings`() {
        val parser = MarkdownParser()
        val content = """
            # Hello World!
            ## Section With Spaces
            ### Code & Symbols
        """.trimIndent()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("test.md"),
            rawContent = content,
            lastModified = Instant.now()
        )

        assertEquals("hello-world", doc.headings[0].anchor)
        assertEquals("section-with-spaces", doc.headings[1].anchor)
        assertEquals("code--symbols", doc.headings[2].anchor)
    }

    @Test
    fun `should parse directory of markdown files`() {
        // Create test files
        val docsDir = tempDir.resolve("docs")
        docsDir.createDirectories()

        docsDir.resolve("index.md").writeText("""
            ---
            title: Home
            ---
            # Welcome
        """.trimIndent())

        docsDir.resolve("guide.md").writeText("""
            # Guide
            Some guide content.
        """.trimIndent())

        val subDir = docsDir.resolve("advanced")
        subDir.createDirectories()
        subDir.resolve("tips.md").writeText("# Tips")

        // Parse directory
        val parser = MarkdownParser()
        val result = parser.parseDirectory(docsDir)

        assertEquals(3, result.documents.size)
        assertEquals(3, result.totalFilesScanned)
        assertEquals(0, result.errors.size)
        assertTrue(result.scanDurationMs >= 0)
    }

    @Test
    fun `should handle parse errors gracefully`() {
        // Create a file that can be read
        val docsDir = tempDir.resolve("docs")
        docsDir.createDirectories()
        docsDir.resolve("valid.md").writeText("# Valid")

        val parser = MarkdownParser()
        val result = parser.parseDirectory(docsDir)

        assertEquals(1, result.successCount)
        assertEquals(0, result.errorCount)
    }

    @Test
    fun `should format slug as title correctly`() {
        val parser = MarkdownParser(TitleSource.FILENAME)

        val testCases = listOf(
            "getting-started.md" to "Getting Started",
            "api_reference.md" to "Api Reference",
            "FAQ.md" to "Faq",
            "simple.md" to "Simple",
            "multi-word-title-here.md" to "Multi Word Title Here"
        )

        for ((filename, expectedTitle) in testCases) {
            val doc = parser.parse(
                rootDir = tempDir,
                relativePath = Path.of(filename),
                rawContent = "# Content",
                lastModified = Instant.now()
            )
            assertEquals(expectedTitle, doc.title, "For filename: $filename")
        }
    }

    @Test
    fun `should identify index files`() {
        val parser = MarkdownParser()

        val indexDoc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("section/index.md"),
            rawContent = "# Index",
            lastModified = Instant.now()
        )

        val regularDoc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("section/page.md"),
            rawContent = "# Page",
            lastModified = Instant.now()
        )

        assertTrue(indexDoc.isIndex)
        assertFalse(regularDoc.isIndex)
    }

    @Test
    fun `should get parent path`() {
        val parser = MarkdownParser()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("docs/guides/intro.md"),
            rawContent = "# Intro",
            lastModified = Instant.now()
        )

        assertEquals(Path.of("docs/guides"), doc.parentPath)
    }

    @Test
    fun `should get slug`() {
        val parser = MarkdownParser()

        val doc = parser.parse(
            rootDir = tempDir,
            relativePath = Path.of("my-document.md"),
            rawContent = "# Doc",
            lastModified = Instant.now()
        )

        assertEquals("my-document", doc.slug)
    }
}
