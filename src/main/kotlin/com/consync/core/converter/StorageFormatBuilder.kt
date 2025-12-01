package com.consync.core.converter

/**
 * Builds Confluence storage format XHTML content.
 *
 * Confluence storage format is XHTML with special Confluence-specific
 * elements and macros using the `ac:` and `ri:` namespaces.
 */
class StorageFormatBuilder {
    private val content = StringBuilder()
    private var indentLevel = 0
    private val indentString = "  "

    /**
     * Append raw content without escaping.
     */
    fun raw(text: String): StorageFormatBuilder {
        content.append(text)
        return this
    }

    /**
     * Append text with XML escaping.
     */
    fun text(text: String): StorageFormatBuilder {
        content.append(escapeXml(text))
        return this
    }

    /**
     * Append a newline.
     */
    fun newline(): StorageFormatBuilder {
        content.append("\n")
        return this
    }

    /**
     * Open a tag with optional attributes.
     */
    fun openTag(tag: String, vararg attributes: Pair<String, String>): StorageFormatBuilder {
        content.append("<$tag")
        for ((name, value) in attributes) {
            content.append(" $name=\"${escapeXml(value)}\"")
        }
        content.append(">")
        return this
    }

    /**
     * Close a tag.
     */
    fun closeTag(tag: String): StorageFormatBuilder {
        content.append("</$tag>")
        return this
    }

    /**
     * Add a self-closing tag.
     */
    fun selfClosingTag(tag: String, vararg attributes: Pair<String, String>): StorageFormatBuilder {
        content.append("<$tag")
        for ((name, value) in attributes) {
            content.append(" $name=\"${escapeXml(value)}\"")
        }
        content.append(" />")
        return this
    }

    /**
     * Add an element with content.
     */
    fun element(tag: String, textContent: String, vararg attributes: Pair<String, String>): StorageFormatBuilder {
        openTag(tag, *attributes)
        text(textContent)
        closeTag(tag)
        return this
    }

    // ========== Common HTML Elements ==========

    fun paragraph(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("p")
        block()
        closeTag("p")
        return this
    }

    fun heading(level: Int, block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("h$level")
        block()
        closeTag("h$level")
        return this
    }

    fun bold(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("strong")
        block()
        closeTag("strong")
        return this
    }

    fun italic(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("em")
        block()
        closeTag("em")
        return this
    }

    fun code(text: String): StorageFormatBuilder {
        return element("code", text)
    }

    fun link(href: String, block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("a", "href" to href)
        block()
        closeTag("a")
        return this
    }

    fun image(src: String, alt: String = "", title: String? = null): StorageFormatBuilder {
        val attrs = mutableListOf("src" to src, "alt" to alt)
        if (title != null) {
            attrs.add("title" to title)
        }
        return selfClosingTag("img", *attrs.toTypedArray())
    }

    fun lineBreak(): StorageFormatBuilder {
        return raw("<br />")
    }

    fun horizontalRule(): StorageFormatBuilder {
        return raw("<hr />")
    }

    // ========== Lists ==========

    fun unorderedList(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("ul")
        block()
        closeTag("ul")
        return this
    }

    fun orderedList(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("ol")
        block()
        closeTag("ol")
        return this
    }

    fun listItem(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("li")
        block()
        closeTag("li")
        return this
    }

    // ========== Tables ==========

    fun table(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("table", "class" to "confluenceTable")
        openTag("tbody")
        block()
        closeTag("tbody")
        closeTag("table")
        return this
    }

    fun tableRow(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("tr")
        block()
        closeTag("tr")
        return this
    }

    fun tableHeader(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("th", "class" to "confluenceTh")
        block()
        closeTag("th")
        return this
    }

    fun tableCell(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("td", "class" to "confluenceTd")
        block()
        closeTag("td")
        return this
    }

    // ========== Blockquote ==========

    fun blockquote(block: StorageFormatBuilder.() -> Unit): StorageFormatBuilder {
        openTag("blockquote")
        block()
        closeTag("blockquote")
        return this
    }

    // ========== Build ==========

    /**
     * Build the final storage format string.
     */
    fun build(): String = content.toString()

    /**
     * Clear the builder for reuse.
     */
    fun clear(): StorageFormatBuilder {
        content.clear()
        return this
    }

    /**
     * Get current length.
     */
    fun length(): Int = content.length

    /**
     * Check if empty.
     */
    fun isEmpty(): Boolean = content.isEmpty()

    companion object {
        /**
         * Escape special XML characters.
         */
        fun escapeXml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
        }

        /**
         * Escape content for CDATA sections (only ]]> needs escaping).
         */
        fun escapeCData(text: String): String {
            return text.replace("]]>", "]]]]><![CDATA[>")
        }
    }
}
