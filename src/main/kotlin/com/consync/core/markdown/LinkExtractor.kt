package com.consync.core.markdown

import org.commonmark.node.*
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension

/**
 * Extracts and resolves links and images from markdown documents.
 */
class LinkExtractor {
    private val logger = LoggerFactory.getLogger(LinkExtractor::class.java)

    /**
     * Extract all links from a parsed markdown document.
     *
     * @param document Parsed CommonMark document
     * @param documentPath Relative path of the source document
     * @return List of extracted links
     */
    fun extractLinks(document: Document, documentPath: Path): List<DocumentLink> {
        val links = mutableListOf<DocumentLink>()
        var lineNumber = 1

        document.accept(object : AbstractVisitor() {
            override fun visit(link: Link) {
                val text = extractLinkText(link)
                val href = link.destination

                val (resolvedPath, isInternal) = resolveLink(href, documentPath)
                val isAnchor = href.startsWith("#")

                links.add(DocumentLink(
                    text = text,
                    href = href,
                    resolvedPath = resolvedPath,
                    isInternal = isInternal,
                    isAnchor = isAnchor,
                    lineNumber = lineNumber
                ))

                super.visit(link)
            }

            override fun visit(paragraph: Paragraph) {
                lineNumber++
                super.visit(paragraph)
            }

            override fun visit(heading: Heading) {
                lineNumber++
                super.visit(heading)
            }
        })

        logger.debug("Extracted ${links.size} links from $documentPath")
        return links
    }

    /**
     * Extract all images from a parsed markdown document.
     *
     * @param document Parsed CommonMark document
     * @param documentPath Relative path of the source document
     * @return List of extracted images
     */
    fun extractImages(document: Document, documentPath: Path): List<DocumentImage> {
        val images = mutableListOf<DocumentImage>()
        var lineNumber = 1

        document.accept(object : AbstractVisitor() {
            override fun visit(image: Image) {
                val src = image.destination
                val (resolvedPath, isLocal) = resolveImagePath(src, documentPath)

                images.add(DocumentImage(
                    altText = extractLinkText(image),
                    src = src,
                    resolvedPath = resolvedPath,
                    isLocal = isLocal,
                    title = image.title,
                    lineNumber = lineNumber
                ))

                super.visit(image)
            }

            override fun visit(paragraph: Paragraph) {
                lineNumber++
                super.visit(paragraph)
            }
        })

        logger.debug("Extracted ${images.size} images from $documentPath")
        return images
    }

    /**
     * Resolve a link href to a path relative to the documentation root.
     *
     * @param href The link href/destination
     * @param documentPath Path of the document containing the link
     * @return Pair of (resolved path or null, whether it's internal)
     */
    fun resolveLink(href: String, documentPath: Path): Pair<Path?, Boolean> {
        // External links
        if (isExternalUrl(href)) {
            return null to false
        }

        // Anchor-only links
        if (href.startsWith("#")) {
            return null to false
        }

        // Email links
        if (href.startsWith("mailto:")) {
            return null to false
        }

        // Remove anchor from href for resolution
        val pathPart = href.substringBefore("#")
        if (pathPart.isEmpty()) {
            return null to false
        }

        // Check if it looks like a markdown file link
        val isMarkdownLink = pathPart.endsWith(".md") ||
                pathPart.endsWith(".markdown") ||
                !pathPart.contains(".") // No extension might be a doc link

        if (!isMarkdownLink) {
            return null to false
        }

        // Resolve relative to document location
        return try {
            val documentDir = documentPath.parent ?: Path("")
            val resolved = documentDir.resolve(pathPart).normalize()

            // Ensure path doesn't escape root (no leading ..)
            if (resolved.toString().startsWith("..")) {
                logger.warn("Link escapes document root: $href in $documentPath")
                null to false
            } else {
                resolved to true
            }
        } catch (e: Exception) {
            logger.warn("Failed to resolve link: $href in $documentPath", e)
            null to false
        }
    }

    /**
     * Resolve an image path.
     *
     * @param src Image source path
     * @param documentPath Path of the document containing the image
     * @return Pair of (resolved path or null, whether it's local)
     */
    fun resolveImagePath(src: String, documentPath: Path): Pair<Path?, Boolean> {
        // External images
        if (isExternalUrl(src)) {
            return null to false
        }

        // Data URLs
        if (src.startsWith("data:")) {
            return null to false
        }

        // Resolve relative to document location
        return try {
            val documentDir = documentPath.parent ?: Path("")
            val resolved = documentDir.resolve(src).normalize()

            // Ensure path doesn't escape root
            if (resolved.toString().startsWith("..")) {
                logger.warn("Image path escapes document root: $src in $documentPath")
                null to false
            } else {
                resolved to true
            }
        } catch (e: Exception) {
            logger.warn("Failed to resolve image path: $src in $documentPath", e)
            null to false
        }
    }

    /**
     * Check if a URL is external (http/https).
     */
    private fun isExternalUrl(url: String): Boolean {
        return url.startsWith("http://") ||
                url.startsWith("https://") ||
                url.startsWith("//")
    }

    /**
     * Extract link text from a Link or Image node.
     */
    private fun extractLinkText(node: Node): String {
        val sb = StringBuilder()

        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                sb.append(text.literal)
            }

            override fun visit(code: Code) {
                sb.append(code.literal)
            }

            override fun visit(emphasis: Emphasis) {
                visitChildren(emphasis)
            }

            override fun visit(strongEmphasis: StrongEmphasis) {
                visitChildren(strongEmphasis)
            }
        })

        return sb.toString().trim()
    }

    /**
     * Find all internal links between documents.
     *
     * @param documents List of parsed documents
     * @return Map of source document path to list of target document paths
     */
    fun buildLinkGraph(documents: List<MarkdownDocument>): Map<Path, List<Path>> {
        val documentPaths = documents.map { it.relativePath }.toSet()
        val linkGraph = mutableMapOf<Path, MutableList<Path>>()

        for (doc in documents) {
            val targets = mutableListOf<Path>()

            for (link in doc.links) {
                if (link.isInternal && link.resolvedPath != null) {
                    // Check if target exists
                    val targetPath = normalizeMarkdownPath(link.resolvedPath)
                    if (targetPath in documentPaths) {
                        targets.add(targetPath)
                    } else {
                        logger.debug("Broken link in ${doc.relativePath}: ${link.href} -> ${link.resolvedPath}")
                    }
                }
            }

            if (targets.isNotEmpty()) {
                linkGraph[doc.relativePath] = targets
            }
        }

        return linkGraph
    }

    /**
     * Normalize a markdown path (ensure .md extension).
     */
    private fun normalizeMarkdownPath(path: Path): Path {
        return if (path.extension.isEmpty()) {
            Path(path.toString() + ".md")
        } else {
            path
        }
    }
}
