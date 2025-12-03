package com.consync.core.converter

/**
 * Renders Confluence-specific macros in storage format.
 *
 * Macros use the `ac:structured-macro` element with parameters
 * and body content.
 */
class MacroRenderer(private val builder: StorageFormatBuilder) {

    /**
     * Render a code block macro.
     *
     * @param code The code content
     * @param language Optional language for syntax highlighting
     * @param title Optional title for the code block
     * @param collapse Whether to collapse the code block
     * @param lineNumbers Whether to show line numbers
     */
    fun codeBlock(
        code: String,
        language: String? = null,
        title: String? = null,
        collapse: Boolean = false,
        lineNumbers: Boolean = false
    ) {
        builder.raw("""<ac:structured-macro ac:name="code">""")

        if (language != null && language.isNotBlank()) {
            macroParameter("language", mapLanguage(language))
        }
        if (title != null) {
            macroParameter("title", title)
        }
        if (collapse) {
            macroParameter("collapse", "true")
        }
        if (lineNumbers) {
            macroParameter("linenumbers", "true")
        }

        builder.raw("<ac:plain-text-body><![CDATA[")
        builder.raw(StorageFormatBuilder.escapeCData(code))
        builder.raw("]]></ac:plain-text-body>")
        builder.raw("</ac:structured-macro>")
    }

    /**
     * Render a Table of Contents macro.
     *
     * @param maxLevel Maximum heading level to include (1-6)
     * @param minLevel Minimum heading level to include (1-6)
     * @param style TOC style (e.g., "none", "disc", "circle", "square")
     */
    fun tableOfContents(
        maxLevel: Int = 3,
        minLevel: Int = 1,
        style: String? = null
    ) {
        builder.raw("""<ac:structured-macro ac:name="toc">""")
        macroParameter("maxLevel", maxLevel.toString())
        macroParameter("minLevel", minLevel.toString())
        if (style != null) {
            macroParameter("style", style)
        }
        builder.raw("</ac:structured-macro>")
    }

    /**
     * Render an Info panel macro.
     */
    fun infoPanel(title: String? = null, content: () -> Unit) {
        panel("info", title, content)
    }

    /**
     * Render a Note panel macro.
     */
    fun notePanel(title: String? = null, content: () -> Unit) {
        panel("note", title, content)
    }

    /**
     * Render a Warning panel macro.
     */
    fun warningPanel(title: String? = null, content: () -> Unit) {
        panel("warning", title, content)
    }

    /**
     * Render a Tip panel macro.
     */
    fun tipPanel(title: String? = null, content: () -> Unit) {
        panel("tip", title, content)
    }

    /**
     * Render a generic panel macro.
     */
    fun panel(type: String, title: String? = null, content: () -> Unit) {
        builder.raw("""<ac:structured-macro ac:name="$type">""")
        if (title != null) {
            macroParameter("title", title)
        }
        builder.raw("<ac:rich-text-body>")
        content()
        builder.raw("</ac:rich-text-body>")
        builder.raw("</ac:structured-macro>")
    }

    /**
     * Render an internal page link.
     *
     * @param pageTitle Title of the target page
     * @param spaceKey Optional space key (for cross-space links)
     * @param linkText Text to display for the link
     * @param anchor Optional anchor within the page
     */
    fun pageLink(
        pageTitle: String,
        spaceKey: String? = null,
        linkText: String? = null,
        anchor: String? = null
    ) {
        builder.raw("<ac:link")
        if (anchor != null) {
            builder.raw(""" ac:anchor="$anchor"""")
        }
        builder.raw(">")

        builder.raw("<ri:page ri:content-title=\"${StorageFormatBuilder.escapeXml(pageTitle)}\"")
        if (spaceKey != null) {
            builder.raw(""" ri:space-key="$spaceKey"""")
        }
        builder.raw(" />")

        if (linkText != null) {
            builder.raw("<ac:plain-text-link-body><![CDATA[")
            builder.raw(StorageFormatBuilder.escapeCData(linkText))
            builder.raw("]]></ac:plain-text-link-body>")
        }

        builder.raw("</ac:link>")
    }

    /**
     * Render an anchor.
     */
    fun anchor(name: String) {
        builder.raw("""<ac:structured-macro ac:name="anchor">""")
        macroParameter("", name)
        builder.raw("</ac:structured-macro>")
    }

    /**
     * Render an attachment image.
     *
     * @param filename Attachment filename
     * @param alt Alt text
     * @param width Optional width
     * @param height Optional height
     */
    fun attachmentImage(
        filename: String,
        alt: String? = null,
        width: Int? = null,
        height: Int? = null
    ) {
        builder.raw("<ac:image")
        if (width != null) builder.raw(""" ac:width="$width"""")
        if (height != null) builder.raw(""" ac:height="$height"""")
        if (alt != null) builder.raw(""" ac:alt="${StorageFormatBuilder.escapeXml(alt)}"""")
        builder.raw(">")
        builder.raw("""<ri:attachment ri:filename="${StorageFormatBuilder.escapeXml(filename)}" />""")
        builder.raw("</ac:image>")
    }

    /**
     * Render an external image.
     */
    fun externalImage(
        url: String,
        alt: String? = null,
        width: Int? = null,
        height: Int? = null
    ) {
        builder.raw("<ac:image")
        if (width != null) builder.raw(""" ac:width="$width"""")
        if (height != null) builder.raw(""" ac:height="$height"""")
        if (alt != null) builder.raw(""" ac:alt="${StorageFormatBuilder.escapeXml(alt)}"""")
        builder.raw(">")
        builder.raw("""<ri:url ri:value="${StorageFormatBuilder.escapeXml(url)}" />""")
        builder.raw("</ac:image>")
    }

    /**
     * Render an Expand macro (collapsible section).
     */
    fun expand(title: String = "Click to expand...", content: () -> Unit) {
        builder.raw("""<ac:structured-macro ac:name="expand">""")
        macroParameter("title", title)
        builder.raw("<ac:rich-text-body>")
        content()
        builder.raw("</ac:rich-text-body>")
        builder.raw("</ac:structured-macro>")
    }

    /**
     * Render a Status macro (colored label).
     */
    fun status(text: String, color: String = "Grey") {
        builder.raw("""<ac:structured-macro ac:name="status">""")
        macroParameter("colour", color)
        macroParameter("title", text)
        builder.raw("</ac:structured-macro>")
    }

    /**
     * Render a task list.
     */
    fun taskList(tasks: List<Pair<String, Boolean>>) {
        builder.raw("<ac:task-list>")
        for ((text, completed) in tasks) {
            builder.raw("<ac:task>")
            builder.raw("<ac:task-status>${if (completed) "complete" else "incomplete"}</ac:task-status>")
            builder.raw("<ac:task-body>")
            builder.text(text)
            builder.raw("</ac:task-body>")
            builder.raw("</ac:task>")
        }
        builder.raw("</ac:task-list>")
    }

    /**
     * Add a macro parameter.
     */
    private fun macroParameter(name: String, value: String) {
        if (name.isEmpty()) {
            builder.raw("""<ac:parameter ac:name="">${StorageFormatBuilder.escapeXml(value)}</ac:parameter>""")
        } else {
            builder.raw("""<ac:parameter ac:name="$name">${StorageFormatBuilder.escapeXml(value)}</ac:parameter>""")
        }
    }

    /**
     * Map markdown language identifiers to Confluence code macro languages.
     */
    private fun mapLanguage(language: String): String {
        return when (language.lowercase()) {
            "js", "javascript" -> "javascript"
            "ts", "typescript" -> "typescript"
            "py", "python" -> "python"
            "rb", "ruby" -> "ruby"
            "kt", "kotlin" -> "kotlin"
            "java" -> "java"
            "scala" -> "scala"
            "go", "golang" -> "go"
            "rs", "rust" -> "rust"
            "c" -> "c"
            "cpp", "c++" -> "cpp"
            "cs", "csharp", "c#" -> "csharp"
            "swift" -> "swift"
            "objc", "objective-c" -> "objc"
            "php" -> "php"
            "pl", "perl" -> "perl"
            "sh", "bash", "shell", "zsh" -> "bash"
            "ps", "powershell" -> "powershell"
            "sql" -> "sql"
            "html" -> "html"
            "xml" -> "xml"
            "css" -> "css"
            "scss", "sass" -> "sass"
            "json" -> "json"
            "yaml", "yml" -> "yaml"
            "md", "markdown" -> "text"
            "dockerfile" -> "dockerfile"
            "groovy" -> "groovy"
            "lua" -> "lua"
            "r" -> "r"
            "matlab" -> "matlab"
            "vb", "vbnet" -> "vb"
            "diff" -> "diff"
            "ini" -> "ini"
            "properties" -> "properties"
            "plaintext", "text", "txt" -> "text"
            else -> language.lowercase()
        }
    }
}
