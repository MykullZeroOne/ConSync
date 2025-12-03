package com.consync.core.converter

import com.consync.config.TocConfig
import com.consync.config.TocPosition
import com.consync.core.hierarchy.PageNode
import com.consync.core.markdown.MarkdownDocument
import org.commonmark.ext.gfm.tables.*
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.slf4j.LoggerFactory

/**
 * Converts Markdown content to Confluence storage format.
 *
 * Uses the CommonMark AST visitor pattern to traverse and convert
 * each node to its Confluence XHTML equivalent.
 */
class ConfluenceConverter(
    private val tocConfig: TocConfig = TocConfig(),
    private val pageResolver: ((String) -> PageNode?)? = null
) {
    private val logger = LoggerFactory.getLogger(ConfluenceConverter::class.java)

    private val parser = Parser.builder()
        .extensions(listOf(
            TablesExtension.create(),
            org.commonmark.ext.task.list.items.TaskListItemsExtension.create(),
            org.commonmark.ext.autolink.AutolinkExtension.create()
        ))
        .build()

    /**
     * Convert a MarkdownDocument to Confluence storage format.
     *
     * @param document The markdown document to convert
     * @return Confluence storage format XHTML string
     */
    fun convert(document: MarkdownDocument): String {
        return convert(document.content)
    }

    /**
     * Convert markdown content string to Confluence storage format.
     *
     * @param markdown The markdown content
     * @return Confluence storage format XHTML string
     */
    fun convert(markdown: String): String {
        val builder = StorageFormatBuilder()
        val macros = MacroRenderer(builder)

        // Add TOC at top if configured
        if (tocConfig.enabled && tocConfig.position == TocPosition.TOP) {
            macros.tableOfContents(maxLevel = tocConfig.depth)
        }

        // Parse and convert
        val document = parser.parse(markdown)
        val visitor = ConfluenceVisitor(builder, macros, pageResolver)
        document.accept(visitor)

        // Add TOC at bottom if configured
        if (tocConfig.enabled && tocConfig.position == TocPosition.BOTTOM) {
            macros.tableOfContents(maxLevel = tocConfig.depth)
        }

        return builder.build()
    }

    /**
     * Convert with custom TOC settings.
     */
    fun convertWithToc(markdown: String, tocConfig: TocConfig): String {
        return ConfluenceConverter(tocConfig, pageResolver).convert(markdown)
    }
}

/**
 * CommonMark visitor that converts nodes to Confluence storage format.
 */
private class ConfluenceVisitor(
    private val builder: StorageFormatBuilder,
    private val macros: MacroRenderer,
    private val pageResolver: ((String) -> PageNode?)?
) : AbstractVisitor() {

    private var inTaskList = false
    private val taskItems = mutableListOf<Pair<String, Boolean>>()

    // ========== Block Elements ==========

    override fun visit(document: Document) {
        visitChildren(document)
    }

    override fun visit(heading: Heading) {
        builder.heading(heading.level) {
            visitChildren(heading)
        }
    }

    override fun visit(paragraph: Paragraph) {
        // Check if this paragraph is inside a tight list item
        val parent = paragraph.parent
        if (parent is ListItem && isTightListItem(parent)) {
            visitChildren(paragraph)
        } else {
            builder.paragraph {
                visitChildren(paragraph)
            }
        }
    }

    override fun visit(blockQuote: BlockQuote) {
        builder.blockquote {
            visitChildren(blockQuote)
        }
    }

    override fun visit(bulletList: BulletList) {
        // Check for task list
        if (isTaskList(bulletList)) {
            inTaskList = true
            taskItems.clear()
            visitChildren(bulletList)
            macros.taskList(taskItems.toList())
            taskItems.clear()
            inTaskList = false
        } else {
            builder.unorderedList {
                visitChildren(bulletList)
            }
        }
    }

    override fun visit(orderedList: OrderedList) {
        builder.orderedList {
            visitChildren(orderedList)
        }
    }

    override fun visit(listItem: ListItem) {
        if (inTaskList) {
            // Extract task item
            val checked = isTaskItemChecked(listItem)
            val text = extractTextContent(listItem)
            taskItems.add(text to checked)
        } else {
            builder.listItem {
                visitChildren(listItem)
            }
        }
    }

    override fun visit(fencedCodeBlock: FencedCodeBlock) {
        macros.codeBlock(
            code = fencedCodeBlock.literal.trimEnd('\n'),
            language = fencedCodeBlock.info.takeIf { it.isNotBlank() }
        )
    }

    override fun visit(indentedCodeBlock: IndentedCodeBlock) {
        macros.codeBlock(
            code = indentedCodeBlock.literal.trimEnd('\n'),
            language = null
        )
    }

    override fun visit(thematicBreak: ThematicBreak) {
        builder.horizontalRule()
    }

    override fun visit(htmlBlock: HtmlBlock) {
        // Pass through HTML blocks as-is (potentially dangerous)
        builder.raw(htmlBlock.literal)
    }

    // ========== Inline Elements ==========

    override fun visit(text: Text) {
        builder.text(text.literal)
    }

    override fun visit(emphasis: Emphasis) {
        builder.italic {
            visitChildren(emphasis)
        }
    }

    override fun visit(strongEmphasis: StrongEmphasis) {
        builder.bold {
            visitChildren(strongEmphasis)
        }
    }

    override fun visit(code: Code) {
        builder.code(code.literal)
    }

    override fun visit(link: Link) {
        val destination = link.destination

        when {
            // Internal markdown link - try to resolve to page
            isInternalMarkdownLink(destination) -> {
                val pageName = extractPageName(destination)
                val resolved = pageResolver?.invoke(pageName)

                if (resolved != null) {
                    macros.pageLink(
                        pageTitle = resolved.title,
                        linkText = extractLinkText(link)
                    )
                } else {
                    // Fallback to regular link if not resolved
                    builder.link(destination) {
                        visitChildren(link)
                    }
                }
            }
            // Anchor link
            destination.startsWith("#") -> {
                builder.link(destination) {
                    visitChildren(link)
                }
            }
            // External link
            else -> {
                builder.link(destination) {
                    visitChildren(link)
                }
            }
        }
    }

    override fun visit(image: Image) {
        val src = image.destination

        when {
            // External image
            src.startsWith("http://") || src.startsWith("https://") -> {
                macros.externalImage(
                    url = src,
                    alt = image.title ?: extractAltText(image)
                )
            }
            // Local image - treat as attachment
            else -> {
                val filename = src.substringAfterLast("/")
                macros.attachmentImage(
                    filename = filename,
                    alt = image.title ?: extractAltText(image)
                )
            }
        }
    }

    override fun visit(softLineBreak: SoftLineBreak) {
        builder.raw(" ")
    }

    override fun visit(hardLineBreak: HardLineBreak) {
        builder.lineBreak()
    }

    override fun visit(htmlInline: HtmlInline) {
        // Pass through inline HTML
        builder.raw(htmlInline.literal)
    }

    // ========== Tables (GFM Extension) ==========

    override fun visit(customBlock: CustomBlock) {
        when (customBlock) {
            is TableBlock -> visitTable(customBlock)
            else -> visitChildren(customBlock)
        }
    }

    private fun visitTable(table: TableBlock) {
        builder.table {
            var node = table.firstChild
            while (node != null) {
                when (node) {
                    is TableHead -> visitTableHead(node)
                    is TableBody -> visitTableBody(node)
                }
                node = node.next
            }
        }
    }

    private fun visitTableHead(head: TableHead) {
        var row = head.firstChild
        while (row != null) {
            if (row is TableRow) {
                builder.tableRow {
                    var cell = row.firstChild
                    while (cell != null) {
                        if (cell is TableCell) {
                            builder.tableHeader {
                                visitChildren(cell)
                            }
                        }
                        cell = cell.next
                    }
                }
            }
            row = row.next
        }
    }

    private fun visitTableBody(body: TableBody) {
        var row = body.firstChild
        while (row != null) {
            if (row is TableRow) {
                builder.tableRow {
                    var cell = row.firstChild
                    while (cell != null) {
                        if (cell is TableCell) {
                            builder.tableCell {
                                visitChildren(cell)
                            }
                        }
                        cell = cell.next
                    }
                }
            }
            row = row.next
        }
    }

    override fun visit(customNode: CustomNode) {
        when (customNode) {
            is TableCell -> {} // Handled in table visitor
            is TaskListItemMarker -> {} // Handled in list visitor
            else -> visitChildren(customNode)
        }
    }

    // ========== Helper Methods ==========

    private fun isInternalMarkdownLink(href: String): Boolean {
        if (href.startsWith("http://") || href.startsWith("https://")) return false
        if (href.startsWith("mailto:")) return false
        if (href.startsWith("#")) return false
        return href.endsWith(".md") || !href.contains(".")
    }

    private fun extractPageName(href: String): String {
        return href
            .substringBefore("#")
            .substringBeforeLast(".")
            .substringAfterLast("/")
    }

    private fun extractLinkText(link: Link): String {
        val sb = StringBuilder()
        var node = link.firstChild
        while (node != null) {
            if (node is Text) sb.append(node.literal)
            node = node.next
        }
        return sb.toString()
    }

    private fun extractAltText(image: Image): String {
        val sb = StringBuilder()
        var node = image.firstChild
        while (node != null) {
            if (node is Text) sb.append(node.literal)
            node = node.next
        }
        return sb.toString()
    }

    private fun extractTextContent(node: Node): String {
        val sb = StringBuilder()
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                sb.append(text.literal)
            }
        })
        return sb.toString().trim()
    }

    private fun isTightListItem(item: ListItem): Boolean {
        val list = item.parent
        return when (list) {
            is BulletList -> list.isTight
            is OrderedList -> list.isTight
            else -> false
        }
    }

    private fun isTaskList(list: BulletList): Boolean {
        var item = list.firstChild
        while (item != null) {
            if (item is ListItem) {
                var child = item.firstChild
                while (child != null) {
                    if (child is Paragraph) {
                        var pChild = child.firstChild
                        while (pChild != null) {
                            if (pChild is TaskListItemMarker) return true
                            pChild = pChild.next
                        }
                    }
                    child = child.next
                }
            }
            item = item.next
        }
        return false
    }

    private fun isTaskItemChecked(item: ListItem): Boolean {
        var child = item.firstChild
        while (child != null) {
            if (child is Paragraph) {
                var pChild = child.firstChild
                while (pChild != null) {
                    if (pChild is TaskListItemMarker) {
                        return pChild.isChecked
                    }
                    pChild = pChild.next
                }
            }
            child = child.next
        }
        return false
    }
}
