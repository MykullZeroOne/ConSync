package com.consync.core.markdown

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrontmatterParserTest {

    private val parser = FrontmatterParser()

    @Test
    fun `should parse simple frontmatter`() {
        val content = """
            ---
            title: My Page Title
            description: A description
            ---
            # Content

            Some text here.
        """.trimIndent()

        val (frontmatter, body) = parser.parse(content)

        assertEquals("My Page Title", frontmatter.title)
        assertEquals("A description", frontmatter.description)
        assertTrue(body.startsWith("# Content"))
    }

    @Test
    fun `should parse frontmatter with tags as list`() {
        val content = """
            ---
            title: Tagged Page
            tags:
              - documentation
              - guide
              - tutorial
            ---
            Content here
        """.trimIndent()

        val (frontmatter, _) = parser.parse(content)

        assertEquals("Tagged Page", frontmatter.title)
        assertEquals(listOf("documentation", "guide", "tutorial"), frontmatter.tags)
    }

    @Test
    fun `should parse frontmatter with tags as comma string`() {
        val content = """
            ---
            title: Tagged Page
            tags: doc, guide, tutorial
            ---
            Content here
        """.trimIndent()

        val (frontmatter, _) = parser.parse(content)

        assertEquals(listOf("doc", "guide", "tutorial"), frontmatter.tags)
    }

    @Test
    fun `should parse all standard frontmatter fields`() {
        val content = """
            ---
            title: Full Example
            description: A complete example
            tags: [a, b]
            author: John Doe
            date: 2024-01-15
            weight: 10
            nav: false
            confluence_id: "12345"
            parent: parent-page
            ---
            Body
        """.trimIndent()

        val (frontmatter, _) = parser.parse(content)

        assertEquals("Full Example", frontmatter.title)
        assertEquals("A complete example", frontmatter.description)
        assertEquals(listOf("a", "b"), frontmatter.tags)
        assertEquals("John Doe", frontmatter.author)
        assertEquals("2024-01-15", frontmatter.date)
        assertEquals(10, frontmatter.weight)
        assertFalse(frontmatter.nav)
        assertEquals("12345", frontmatter.confluenceId)
        assertEquals("parent-page", frontmatter.parent)
    }

    @Test
    fun `should handle content without frontmatter`() {
        val content = """
            # Just a Heading

            Some content without frontmatter.
        """.trimIndent()

        val (frontmatter, body) = parser.parse(content)

        assertEquals(Frontmatter.EMPTY, frontmatter)
        assertEquals(content, body)
    }

    @Test
    fun `should handle empty frontmatter`() {
        val content = """
            ---
            ---
            # Content
        """.trimIndent()

        val (frontmatter, body) = parser.parse(content)

        assertEquals(Frontmatter.EMPTY, frontmatter)
        assertTrue(body.contains("# Content"))
    }

    @Test
    fun `should preserve custom properties`() {
        val content = """
            ---
            title: Custom Props
            custom_field: custom value
            another_custom: 123
            ---
            Body
        """.trimIndent()

        val (frontmatter, _) = parser.parse(content)

        assertEquals("Custom Props", frontmatter.title)
        assertEquals("custom value", frontmatter.custom["custom_field"])
        assertEquals("123", frontmatter.custom["another_custom"].toString())
    }

    @Test
    fun `should detect frontmatter presence`() {
        val withFrontmatter = """
            ---
            title: Test
            ---
            Content
        """.trimIndent()

        val withoutFrontmatter = """
            # Just content
            No frontmatter
        """.trimIndent()

        assertTrue(parser.hasFrontmatter(withFrontmatter))
        assertFalse(parser.hasFrontmatter(withoutFrontmatter))
    }

    @Test
    fun `should handle unclosed frontmatter gracefully`() {
        val content = """
            ---
            title: Unclosed
            # This looks like frontmatter but has no closing delimiter
        """.trimIndent()

        val (frontmatter, body) = parser.parse(content)

        // Should return original content when frontmatter is malformed
        assertEquals(Frontmatter.EMPTY, frontmatter)
    }

    @Test
    fun `should strip frontmatter correctly`() {
        val content = """
            ---
            title: Test
            ---

            # Heading

            Content
        """.trimIndent()

        val stripped = parser.stripFrontmatter(content)

        assertFalse(stripped.contains("---"))
        assertFalse(stripped.contains("title:"))
        assertTrue(stripped.contains("# Heading"))
    }

    @Test
    fun `should extract frontmatter yaml`() {
        val content = """
            ---
            title: Extract Test
            tags: [a, b]
            ---
            Body
        """.trimIndent()

        val yaml = parser.extractFrontmatterYaml(content)

        assertEquals("title: Extract Test\ntags: [a, b]", yaml)
    }

    @Test
    fun `should return null for content without frontmatter`() {
        val content = "Just plain content"

        val yaml = parser.extractFrontmatterYaml(content)

        assertNull(yaml)
    }

    @Test
    fun `should handle frontmatter with special characters`() {
        val content = """
            ---
            title: "Title with: colon"
            description: "Contains \"quotes\""
            ---
            Content
        """.trimIndent()

        val (frontmatter, _) = parser.parse(content)

        assertEquals("Title with: colon", frontmatter.title)
        assertEquals("Contains \"quotes\"", frontmatter.description)
    }
}
