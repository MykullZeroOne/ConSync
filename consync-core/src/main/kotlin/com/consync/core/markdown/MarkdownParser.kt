package com.consync.core.markdown

import com.consync.client.filesystem.FileScanner
import com.consync.config.FilesConfig
import com.consync.config.TitleSource
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.ext.autolink.AutolinkExtension
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.io.path.name

/**
 * Parses markdown files into MarkdownDocument objects.
 */
class MarkdownParser(
    private val titleSource: TitleSource = TitleSource.FILENAME
) {
    private val logger = LoggerFactory.getLogger(MarkdownParser::class.java)

    private val frontmatterParser = FrontmatterParser()
    private val linkExtractor = LinkExtractor()

    private val parser: Parser = Parser.builder()
        .extensions(listOf(
            TablesExtension.create(),
            TaskListItemsExtension.create(),
            AutolinkExtension.create()
        ))
        .build()

    /**
     * Parse a single markdown file.
     *
     * @param rootDir Root documentation directory
     * @param relativePath Relative path to the file
     * @param rawContent Raw file content
     * @param lastModified Last modification time
     * @return Parsed MarkdownDocument
     */
    fun parse(
        rootDir: Path,
        relativePath: Path,
        rawContent: String,
        lastModified: Instant
    ): MarkdownDocument {
        logger.debug("Parsing: $relativePath")

        // Parse frontmatter
        val (frontmatter, contentWithoutFrontmatter) = frontmatterParser.parse(rawContent)

        // Parse markdown AST
        val document = parser.parse(contentWithoutFrontmatter) as Document

        // Extract components
        val headings = extractHeadings(document)
        val links = linkExtractor.extractLinks(document, relativePath)
        val images = linkExtractor.extractImages(document, relativePath)

        // Determine title
        val title = determineTitle(frontmatter, headings, relativePath)

        // Calculate content hash
        val contentHash = calculateHash(rawContent)

        return MarkdownDocument(
            relativePath = relativePath,
            absolutePath = rootDir.resolve(relativePath),
            rawContent = rawContent,
            content = contentWithoutFrontmatter,
            frontmatter = frontmatter,
            title = title,
            links = links,
            images = images,
            headings = headings,
            lastModified = lastModified,
            contentHash = contentHash
        )
    }

    /**
     * Parse all markdown files in a directory.
     *
     * @param rootDir Root directory to scan
     * @param filesConfig File scanning configuration
     * @return ScanResult with all parsed documents
     */
    fun parseDirectory(
        rootDir: Path,
        filesConfig: FilesConfig = FilesConfig()
    ): ScanResult {
        val startTime = System.currentTimeMillis()
        val fileScanner = FileScanner(filesConfig)

        val files = fileScanner.scan(rootDir)
        val documents = mutableListOf<MarkdownDocument>()
        val errors = mutableListOf<ScanError>()

        for (relativePath in files) {
            try {
                val content = fileScanner.readFile(rootDir, relativePath)
                val lastModified = fileScanner.getLastModified(rootDir, relativePath)
                val doc = parse(rootDir, relativePath, content, lastModified)
                documents.add(doc)
            } catch (e: Exception) {
                logger.error("Failed to parse: $relativePath", e)
                errors.add(ScanError(
                    path = relativePath,
                    message = e.message ?: "Unknown error",
                    exception = e
                ))
            }
        }

        val duration = System.currentTimeMillis() - startTime
        logger.info("Parsed ${documents.size} documents in ${duration}ms (${errors.size} errors)")

        return ScanResult(
            rootDirectory = rootDir,
            documents = documents,
            errors = errors,
            totalFilesScanned = files.size,
            scanDurationMs = duration
        )
    }

    /**
     * Extract headings from parsed document.
     */
    private fun extractHeadings(document: Document): List<DocumentHeading> {
        val headings = mutableListOf<DocumentHeading>()
        var lineNumber = 1

        document.accept(object : AbstractVisitor() {
            override fun visit(heading: Heading) {
                val text = extractText(heading)
                val anchor = generateAnchor(text)

                headings.add(DocumentHeading(
                    level = heading.level,
                    text = text,
                    anchor = anchor,
                    lineNumber = lineNumber
                ))

                super.visit(heading)
            }

            override fun visit(paragraph: Paragraph) {
                lineNumber++
                super.visit(paragraph)
            }
        })

        return headings
    }

    /**
     * Determine the document title based on configuration.
     */
    private fun determineTitle(
        frontmatter: Frontmatter,
        headings: List<DocumentHeading>,
        relativePath: Path
    ): String {
        return when (titleSource) {
            TitleSource.FRONTMATTER -> {
                frontmatter.title
                    ?: headings.firstOrNull { it.level == 1 }?.text
                    ?: titleFromFilename(relativePath)
            }
            TitleSource.FIRST_HEADING -> {
                headings.firstOrNull { it.level == 1 }?.text
                    ?: frontmatter.title
                    ?: titleFromFilename(relativePath)
            }
            TitleSource.FILENAME -> {
                titleFromFilename(relativePath)
            }
        }
    }

    /**
     * Generate title from filename.
     */
    private fun titleFromFilename(path: Path): String {
        val filename = path.fileName.toString()
        val nameWithoutExt = filename.substringBeforeLast(".")

        // Handle index files - use parent directory name
        if (nameWithoutExt.equals("index", ignoreCase = true)) {
            val parent = path.parent
            return if (parent != null && parent.nameCount > 0) {
                formatTitle(parent.fileName.toString())
            } else {
                "Home"
            }
        }

        return formatTitle(nameWithoutExt)
    }

    /**
     * Format a slug/filename as a title.
     * Converts kebab-case and snake_case to Title Case.
     */
    private fun formatTitle(slug: String): String {
        return slug
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * Extract plain text from a node.
     */
    private fun extractText(node: Node): String {
        val sb = StringBuilder()

        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                sb.append(text.literal)
            }

            override fun visit(code: Code) {
                sb.append(code.literal)
            }

            override fun visit(softLineBreak: SoftLineBreak) {
                sb.append(" ")
            }

            override fun visit(hardLineBreak: HardLineBreak) {
                sb.append(" ")
            }
        })

        return sb.toString().trim()
    }

    /**
     * Generate anchor ID from heading text.
     */
    private fun generateAnchor(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')
    }

    /**
     * Calculate SHA-256 hash of content.
     */
    private fun calculateHash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
