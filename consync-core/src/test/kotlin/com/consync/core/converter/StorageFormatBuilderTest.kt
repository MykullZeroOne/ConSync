package com.consync.core.converter

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageFormatBuilderTest {

    @Test
    fun `should escape XML characters`() {
        val escaped = StorageFormatBuilder.escapeXml("<tag attr=\"value\" & 'quote'>")
        assertEquals("&lt;tag attr=&quot;value&quot; &amp; &apos;quote&apos;&gt;", escaped)
    }

    @Test
    fun `should escape CDATA content`() {
        val escaped = StorageFormatBuilder.escapeCData("content with ]]> in it")
        assertEquals("content with ]]]]><![CDATA[> in it", escaped)
    }

    @Test
    fun `should build simple paragraph`() {
        val builder = StorageFormatBuilder()
        builder.paragraph { text("Hello World") }

        assertEquals("<p>Hello World</p>", builder.build())
    }

    @Test
    fun `should build heading`() {
        val builder = StorageFormatBuilder()
        builder.heading(2) { text("Section Title") }

        assertEquals("<h2>Section Title</h2>", builder.build())
    }

    @Test
    fun `should build formatted text`() {
        val builder = StorageFormatBuilder()
        builder.paragraph {
            text("This is ")
            bold { text("bold") }
            text(" and ")
            italic { text("italic") }
            text(" text.")
        }

        val result = builder.build()
        assertTrue(result.contains("<strong>bold</strong>"))
        assertTrue(result.contains("<em>italic</em>"))
    }

    @Test
    fun `should build inline code`() {
        val builder = StorageFormatBuilder()
        builder.paragraph {
            text("Use ")
            code("git commit")
            text(" to commit.")
        }

        assertTrue(builder.build().contains("<code>git commit</code>"))
    }

    @Test
    fun `should build link`() {
        val builder = StorageFormatBuilder()
        builder.link("https://example.com") {
            text("Example")
        }

        assertEquals("<a href=\"https://example.com\">Example</a>", builder.build())
    }

    @Test
    fun `should build image`() {
        val builder = StorageFormatBuilder()
        builder.image("image.png", "Alt text", "Title")

        val result = builder.build()
        assertTrue(result.contains("src=\"image.png\""))
        assertTrue(result.contains("alt=\"Alt text\""))
        assertTrue(result.contains("title=\"Title\""))
    }

    @Test
    fun `should build unordered list`() {
        val builder = StorageFormatBuilder()
        builder.unorderedList {
            listItem { text("Item 1") }
            listItem { text("Item 2") }
        }

        val result = builder.build()
        assertTrue(result.contains("<ul>"))
        assertTrue(result.contains("<li>Item 1</li>"))
        assertTrue(result.contains("<li>Item 2</li>"))
        assertTrue(result.contains("</ul>"))
    }

    @Test
    fun `should build ordered list`() {
        val builder = StorageFormatBuilder()
        builder.orderedList {
            listItem { text("First") }
            listItem { text("Second") }
        }

        val result = builder.build()
        assertTrue(result.contains("<ol>"))
        assertTrue(result.contains("<li>First</li>"))
        assertTrue(result.contains("</ol>"))
    }

    @Test
    fun `should build table`() {
        val builder = StorageFormatBuilder()
        builder.table {
            tableRow {
                tableHeader { text("Name") }
                tableHeader { text("Value") }
            }
            tableRow {
                tableCell { text("foo") }
                tableCell { text("bar") }
            }
        }

        val result = builder.build()
        assertTrue(result.contains("<table class=\"confluenceTable\">"))
        assertTrue(result.contains("<th class=\"confluenceTh\">Name</th>"))
        assertTrue(result.contains("<td class=\"confluenceTd\">foo</td>"))
    }

    @Test
    fun `should build blockquote`() {
        val builder = StorageFormatBuilder()
        builder.blockquote {
            paragraph { text("Quoted text") }
        }

        val result = builder.build()
        assertTrue(result.contains("<blockquote>"))
        assertTrue(result.contains("Quoted text"))
    }

    @Test
    fun `should append raw content`() {
        val builder = StorageFormatBuilder()
        builder.raw("<custom-element />")

        assertEquals("<custom-element />", builder.build())
    }

    @Test
    fun `should open and close tags`() {
        val builder = StorageFormatBuilder()
        builder.openTag("div", "class" to "container")
        builder.text("Content")
        builder.closeTag("div")

        assertEquals("<div class=\"container\">Content</div>", builder.build())
    }

    @Test
    fun `should create self-closing tags`() {
        val builder = StorageFormatBuilder()
        builder.selfClosingTag("br")

        assertEquals("<br />", builder.build())
    }

    @Test
    fun `should clear builder`() {
        val builder = StorageFormatBuilder()
        builder.text("content")
        builder.clear()

        assertTrue(builder.isEmpty())
        assertEquals("", builder.build())
    }

    @Test
    fun `should report length`() {
        val builder = StorageFormatBuilder()
        builder.text("12345")

        assertEquals(5, builder.length())
    }
}
