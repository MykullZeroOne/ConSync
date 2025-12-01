package com.consync.core.markdown

import java.nio.file.Path
import java.time.Instant

/**
 * Represents a parsed Markdown document with metadata.
 */
data class MarkdownDocument(
    /** Relative path from the documentation root */
    val relativePath: Path,

    /** Absolute path to the file */
    val absolutePath: Path,

    /** Raw markdown content */
    val rawContent: String,

    /** Content without frontmatter */
    val content: String,

    /** Parsed frontmatter metadata */
    val frontmatter: Frontmatter,

    /** Title of the document (from frontmatter, first heading, or filename) */
    val title: String,

    /** Internal links found in the document */
    val links: List<DocumentLink>,

    /** Images referenced in the document */
    val images: List<DocumentImage>,

    /** First-level headings for TOC */
    val headings: List<DocumentHeading>,

    /** File last modified timestamp */
    val lastModified: Instant,

    /** Content hash for change detection */
    val contentHash: String
) {
    /** Check if this is an index file (index.md) */
    val isIndex: Boolean
        get() = relativePath.fileName.toString().equals("index.md", ignoreCase = true)

    /** Get the parent directory path */
    val parentPath: Path?
        get() = relativePath.parent

    /** Get the slug (filename without extension) */
    val slug: String
        get() = relativePath.fileName.toString().substringBeforeLast(".")
}

/**
 * Parsed frontmatter from a Markdown document.
 */
data class Frontmatter(
    /** Document title */
    val title: String? = null,

    /** Document description/summary */
    val description: String? = null,

    /** Tags/labels for the page */
    val tags: List<String> = emptyList(),

    /** Author information */
    val author: String? = null,

    /** Creation date */
    val date: String? = null,

    /** Custom page order/weight */
    val weight: Int? = null,

    /** Whether to include in navigation */
    val nav: Boolean = true,

    /** Custom Confluence page ID (for existing pages) */
    val confluenceId: String? = null,

    /** Custom parent page specification */
    val parent: String? = null,

    /** Additional custom properties */
    val custom: Map<String, Any> = emptyMap()
) {
    companion object {
        val EMPTY = Frontmatter()
    }
}

/**
 * Represents an internal link to another markdown document.
 */
data class DocumentLink(
    /** Original link text */
    val text: String,

    /** Original href/path from markdown */
    val href: String,

    /** Resolved relative path (if internal) */
    val resolvedPath: Path?,

    /** Whether this is an internal link to another doc */
    val isInternal: Boolean,

    /** Whether this is an anchor link (#section) */
    val isAnchor: Boolean,

    /** Line number where link appears */
    val lineNumber: Int
) {
    /** Check if link is external (http/https) */
    val isExternal: Boolean
        get() = href.startsWith("http://") || href.startsWith("https://")
}

/**
 * Represents an image reference in the document.
 */
data class DocumentImage(
    /** Alt text */
    val altText: String,

    /** Original src path */
    val src: String,

    /** Resolved local path (if local image) */
    val resolvedPath: Path?,

    /** Whether this is a local image file */
    val isLocal: Boolean,

    /** Optional title */
    val title: String?,

    /** Line number where image appears */
    val lineNumber: Int
)

/**
 * Represents a heading in the document.
 */
data class DocumentHeading(
    /** Heading level (1-6) */
    val level: Int,

    /** Heading text */
    val text: String,

    /** Generated anchor ID */
    val anchor: String,

    /** Line number */
    val lineNumber: Int
)

/**
 * Result of scanning a directory for markdown files.
 */
data class ScanResult(
    /** Root directory that was scanned */
    val rootDirectory: Path,

    /** All discovered markdown documents */
    val documents: List<MarkdownDocument>,

    /** Files that failed to parse */
    val errors: List<ScanError>,

    /** Total files scanned */
    val totalFilesScanned: Int,

    /** Scan duration in milliseconds */
    val scanDurationMs: Long
) {
    /** Number of successfully parsed documents */
    val successCount: Int
        get() = documents.size

    /** Number of failed documents */
    val errorCount: Int
        get() = errors.size
}

/**
 * Error during document scanning.
 */
data class ScanError(
    /** Path to the file that failed */
    val path: Path,

    /** Error message */
    val message: String,

    /** Exception if available */
    val exception: Throwable? = null
)
