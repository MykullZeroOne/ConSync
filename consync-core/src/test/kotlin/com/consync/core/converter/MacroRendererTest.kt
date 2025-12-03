package com.consync.core.converter

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class MacroRendererTest {

    private lateinit var builder: StorageFormatBuilder
    private lateinit var macros: MacroRenderer

    @BeforeEach
    fun setup() {
        builder = StorageFormatBuilder()
        macros = MacroRenderer(builder)
    }

    @Test
    fun `should render code block`() {
        macros.codeBlock("println(\"Hello\")", language = "kotlin")

        val result = builder.build()
        assertTrue(result.contains("<ac:structured-macro ac:name=\"code\">"))
        assertTrue(result.contains("<ac:parameter ac:name=\"language\">kotlin</ac:parameter>"))
        assertTrue(result.contains("<![CDATA[println(\"Hello\")]]>"))
        assertTrue(result.contains("</ac:structured-macro>"))
    }

    @Test
    fun `should render code block without language`() {
        macros.codeBlock("plain code")

        val result = builder.build()
        assertTrue(result.contains("<ac:structured-macro ac:name=\"code\">"))
        assertFalse(result.contains("language"))
        assertTrue(result.contains("plain code"))
    }

    @Test
    fun `should render code block with options`() {
        macros.codeBlock(
            code = "code",
            language = "java",
            title = "Example",
            collapse = true,
            lineNumbers = true
        )

        val result = builder.build()
        assertTrue(result.contains("<ac:parameter ac:name=\"title\">Example</ac:parameter>"))
        assertTrue(result.contains("<ac:parameter ac:name=\"collapse\">true</ac:parameter>"))
        assertTrue(result.contains("<ac:parameter ac:name=\"linenumbers\">true</ac:parameter>"))
    }

    @Test
    fun `should map language aliases`() {
        macros.codeBlock("code", language = "js")
        assertTrue(builder.build().contains("javascript"))

        builder.clear()
        macros.codeBlock("code", language = "py")
        assertTrue(builder.build().contains("python"))

        builder.clear()
        macros.codeBlock("code", language = "sh")
        assertTrue(builder.build().contains("bash"))
    }

    @Test
    fun `should escape CDATA in code blocks`() {
        macros.codeBlock("code with ]]> special chars")

        val result = builder.build()
        // ]]> should be escaped in CDATA
        assertTrue(result.contains("]]]]><![CDATA[>"))
    }

    @Test
    fun `should render table of contents`() {
        macros.tableOfContents(maxLevel = 4, minLevel = 2)

        val result = builder.build()
        assertTrue(result.contains("<ac:structured-macro ac:name=\"toc\">"))
        assertTrue(result.contains("<ac:parameter ac:name=\"maxLevel\">4</ac:parameter>"))
        assertTrue(result.contains("<ac:parameter ac:name=\"minLevel\">2</ac:parameter>"))
    }

    @Test
    fun `should render info panel`() {
        macros.infoPanel("Note") {
            builder.text("Important information")
        }

        val result = builder.build()
        assertTrue(result.contains("<ac:structured-macro ac:name=\"info\">"))
        assertTrue(result.contains("<ac:parameter ac:name=\"title\">Note</ac:parameter>"))
        assertTrue(result.contains("<ac:rich-text-body>"))
        assertTrue(result.contains("Important information"))
    }

    @Test
    fun `should render warning panel`() {
        macros.warningPanel {
            builder.text("Warning message")
        }

        val result = builder.build()
        assertTrue(result.contains("<ac:structured-macro ac:name=\"warning\">"))
    }

    @Test
    fun `should render page link`() {
        macros.pageLink(
            pageTitle = "Target Page",
            linkText = "Click here"
        )

        val result = builder.build()
        assertTrue(result.contains("<ac:link>"))
        assertTrue(result.contains("<ri:page ri:content-title=\"Target Page\""))
        assertTrue(result.contains("<![CDATA[Click here]]>"))
    }

    @Test
    fun `should render page link with space key`() {
        macros.pageLink(
            pageTitle = "Page",
            spaceKey = "DOCS",
            linkText = "Link"
        )

        val result = builder.build()
        assertTrue(result.contains("ri:space-key=\"DOCS\""))
    }

    @Test
    fun `should render page link with anchor`() {
        macros.pageLink(
            pageTitle = "Page",
            anchor = "section-1"
        )

        val result = builder.build()
        assertTrue(result.contains("ac:anchor=\"section-1\""))
    }

    @Test
    fun `should render attachment image`() {
        macros.attachmentImage(
            filename = "diagram.png",
            alt = "Architecture diagram",
            width = 800
        )

        val result = builder.build()
        assertTrue(result.contains("<ac:image"))
        assertTrue(result.contains("ac:width=\"800\""))
        assertTrue(result.contains("ac:alt=\"Architecture diagram\""))
        assertTrue(result.contains("<ri:attachment ri:filename=\"diagram.png\""))
    }

    @Test
    fun `should render external image`() {
        macros.externalImage(
            url = "https://example.com/image.png",
            alt = "External"
        )

        val result = builder.build()
        assertTrue(result.contains("<ac:image"))
        assertTrue(result.contains("<ri:url ri:value=\"https://example.com/image.png\""))
    }

    @Test
    fun `should render expand macro`() {
        macros.expand("Show details") {
            builder.text("Hidden content")
        }

        val result = builder.build()
        assertTrue(result.contains("<ac:structured-macro ac:name=\"expand\">"))
        assertTrue(result.contains("<ac:parameter ac:name=\"title\">Show details</ac:parameter>"))
        assertTrue(result.contains("Hidden content"))
    }

    @Test
    fun `should render status macro`() {
        macros.status("In Progress", "Yellow")

        val result = builder.build()
        assertTrue(result.contains("<ac:structured-macro ac:name=\"status\">"))
        assertTrue(result.contains("<ac:parameter ac:name=\"colour\">Yellow</ac:parameter>"))
        assertTrue(result.contains("<ac:parameter ac:name=\"title\">In Progress</ac:parameter>"))
    }

    @Test
    fun `should render task list`() {
        macros.taskList(listOf(
            "Task 1" to true,
            "Task 2" to false
        ))

        val result = builder.build()
        assertTrue(result.contains("<ac:task-list>"))
        assertTrue(result.contains("<ac:task-status>complete</ac:task-status>"))
        assertTrue(result.contains("<ac:task-status>incomplete</ac:task-status>"))
        assertTrue(result.contains("Task 1"))
        assertTrue(result.contains("Task 2"))
    }

    @Test
    fun `should escape special characters in macro parameters`() {
        macros.pageLink(
            pageTitle = "Page with \"quotes\" & <symbols>",
            linkText = "Link"
        )

        val result = builder.build()
        assertTrue(result.contains("&quot;quotes&quot;"))
        assertTrue(result.contains("&amp;"))
        assertTrue(result.contains("&lt;symbols&gt;"))
    }
}
