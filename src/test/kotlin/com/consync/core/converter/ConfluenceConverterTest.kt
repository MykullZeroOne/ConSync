package com.consync.core.converter

import com.consync.config.TocConfig
import com.consync.config.TocPosition
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class ConfluenceConverterTest {

    private val converter = ConfluenceConverter(
        tocConfig = TocConfig(enabled = false)
    )

    @Test
    fun `should convert heading`() {
        val result = converter.convert("# Hello World")

        assertTrue(result.contains("<h1>Hello World</h1>"))
    }

    @Test
    fun `should convert multiple heading levels`() {
        val markdown = """
            # H1
            ## H2
            ### H3
            #### H4
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<h1>H1</h1>"))
        assertTrue(result.contains("<h2>H2</h2>"))
        assertTrue(result.contains("<h3>H3</h3>"))
        assertTrue(result.contains("<h4>H4</h4>"))
    }

    @Test
    fun `should convert paragraph`() {
        val result = converter.convert("This is a paragraph.")

        assertTrue(result.contains("<p>This is a paragraph.</p>"))
    }

    @Test
    fun `should convert bold text`() {
        val result = converter.convert("This is **bold** text.")

        assertTrue(result.contains("<strong>bold</strong>"))
    }

    @Test
    fun `should convert italic text`() {
        val result = converter.convert("This is *italic* text.")

        assertTrue(result.contains("<em>italic</em>"))
    }

    @Test
    fun `should convert inline code`() {
        val result = converter.convert("Use `git status` command.")

        assertTrue(result.contains("<code>git status</code>"))
    }

    @Test
    fun `should convert fenced code block`() {
        val markdown = """
            ```kotlin
            fun main() {
                println("Hello")
            }
            ```
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<ac:structured-macro ac:name=\"code\">"))
        assertTrue(result.contains("<ac:parameter ac:name=\"language\">kotlin</ac:parameter>"))
        assertTrue(result.contains("println"))
    }

    @Test
    fun `should convert code block without language`() {
        val markdown = """
            ```
            plain code
            ```
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<ac:structured-macro ac:name=\"code\">"))
        assertFalse(result.contains("language"))
    }

    @Test
    fun `should convert unordered list`() {
        val markdown = """
            - Item 1
            - Item 2
            - Item 3
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<ul>"))
        assertTrue(result.contains("<li>"))
        assertTrue(result.contains("Item 1"))
        assertTrue(result.contains("</ul>"))
    }

    @Test
    fun `should convert ordered list`() {
        val markdown = """
            1. First
            2. Second
            3. Third
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<ol>"))
        assertTrue(result.contains("<li>"))
        assertTrue(result.contains("First"))
    }

    @Test
    fun `should convert nested list`() {
        val markdown = """
            - Parent
              - Child 1
              - Child 2
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<ul>"))
        assertTrue(result.contains("Parent"))
        assertTrue(result.contains("Child 1"))
    }

    @Test
    fun `should convert external link`() {
        val result = converter.convert("[Example](https://example.com)")

        assertTrue(result.contains("<a href=\"https://example.com\">Example</a>"))
    }

    @Test
    fun `should convert external image`() {
        val result = converter.convert("![Alt](https://example.com/image.png)")

        assertTrue(result.contains("<ac:image"))
        assertTrue(result.contains("<ri:url ri:value=\"https://example.com/image.png\""))
    }

    @Test
    fun `should convert local image as attachment`() {
        val result = converter.convert("![Diagram](./images/diagram.png)")

        assertTrue(result.contains("<ac:image"))
        assertTrue(result.contains("<ri:attachment ri:filename=\"diagram.png\""))
    }

    @Test
    fun `should convert blockquote`() {
        val markdown = """
            > This is a quote.
            > Multiple lines.
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<blockquote>"))
        assertTrue(result.contains("This is a quote."))
    }

    @Test
    fun `should convert horizontal rule`() {
        val result = converter.convert("---")

        assertTrue(result.contains("<hr />"))
    }

    @Test
    fun `should convert table`() {
        val markdown = """
            | Name | Value |
            |------|-------|
            | foo  | bar   |
            | baz  | qux   |
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<table class=\"confluenceTable\">"))
        assertTrue(result.contains("<th class=\"confluenceTh\">Name</th>"))
        assertTrue(result.contains("<td class=\"confluenceTd\">foo</td>"))
    }

    @Test
    fun `should convert task list`() {
        val markdown = """
            - [x] Completed task
            - [ ] Incomplete task
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<ac:task-list>"))
        assertTrue(result.contains("<ac:task-status>complete</ac:task-status>"))
        assertTrue(result.contains("<ac:task-status>incomplete</ac:task-status>"))
    }

    @Test
    fun `should add TOC at top when configured`() {
        val converterWithToc = ConfluenceConverter(
            tocConfig = TocConfig(enabled = true, position = TocPosition.TOP, depth = 3)
        )

        val result = converterWithToc.convert("# Heading")

        assertTrue(result.indexOf("<ac:structured-macro ac:name=\"toc\">") < result.indexOf("<h1>"))
    }

    @Test
    fun `should add TOC at bottom when configured`() {
        val converterWithToc = ConfluenceConverter(
            tocConfig = TocConfig(enabled = true, position = TocPosition.BOTTOM, depth = 3)
        )

        val result = converterWithToc.convert("# Heading")

        assertTrue(result.indexOf("<h1>") < result.indexOf("<ac:structured-macro ac:name=\"toc\">"))
    }

    @Test
    fun `should not add TOC when disabled`() {
        val result = converter.convert("# Heading")

        assertFalse(result.contains("<ac:structured-macro ac:name=\"toc\">"))
    }

    @Test
    fun `should escape special characters in text`() {
        val result = converter.convert("Text with <angle> & \"quotes\"")

        assertTrue(result.contains("&lt;angle&gt;"))
        assertTrue(result.contains("&amp;"))
        assertTrue(result.contains("&quot;quotes&quot;"))
    }

    @Test
    fun `should convert hard line break`() {
        val markdown = "Line 1  \nLine 2"  // Two spaces before newline
        val result = converter.convert(markdown)

        assertTrue(result.contains("<br />"))
    }

    @Test
    fun `should convert complete document`() {
        val markdown = """
            # Documentation

            This is the **introduction** with `code`.

            ## Features

            - Feature 1
            - Feature 2

            ```java
            public class Main {}
            ```

            | Column 1 | Column 2 |
            |----------|----------|
            | A        | B        |

            > Important note

            [Link](https://example.com)
        """.trimIndent()

        val result = converter.convert(markdown)

        assertTrue(result.contains("<h1>Documentation</h1>"))
        assertTrue(result.contains("<strong>introduction</strong>"))
        assertTrue(result.contains("<code>code</code>"))
        assertTrue(result.contains("<h2>Features</h2>"))
        assertTrue(result.contains("<ul>"))
        assertTrue(result.contains("<ac:structured-macro ac:name=\"code\">"))
        assertTrue(result.contains("<table"))
        assertTrue(result.contains("<blockquote>"))
        assertTrue(result.contains("<a href="))
    }

    @Test
    fun `should handle empty content`() {
        val result = converter.convert("")
        assertEquals("", result)
    }

    @Test
    fun `should handle whitespace only content`() {
        val result = converter.convert("   \n\n   ")
        // Should not throw, may produce minimal output
        assertTrue(result.isEmpty() || result.isBlank() || result.contains("<p>"))
    }
}
