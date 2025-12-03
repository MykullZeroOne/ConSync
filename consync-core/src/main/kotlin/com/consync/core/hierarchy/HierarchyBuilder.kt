package com.consync.core.hierarchy

import com.consync.core.markdown.MarkdownDocument
import com.consync.core.markdown.ScanResult
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

/**
 * Builds a page hierarchy tree from parsed markdown documents.
 *
 * The hierarchy is derived from the directory structure:
 * - Directories become parent pages
 * - Files within directories become child pages
 * - index.md files provide content for directory pages
 * - Files are sorted by frontmatter weight, then alphabetically
 */
class HierarchyBuilder(
    private val rootTitle: String = "Home",
    private val indexFileName: String = "index.md"
) {
    private val logger = LoggerFactory.getLogger(HierarchyBuilder::class.java)

    /**
     * Build hierarchy from scan result.
     *
     * @param scanResult Result from MarkdownParser.parseDirectory()
     * @return HierarchyResult with the complete tree
     */
    fun build(scanResult: ScanResult): HierarchyResult {
        return build(scanResult.documents)
    }

    /**
     * Build hierarchy from list of documents.
     *
     * @param documents List of parsed markdown documents
     * @return HierarchyResult with the complete tree
     */
    fun build(documents: List<MarkdownDocument>): HierarchyResult {
        logger.info("Building hierarchy from ${documents.size} documents")

        val nodesByPath = mutableMapOf<Path, PageNode>()
        val nodesById = mutableMapOf<String, PageNode>()
        val virtualNodes = mutableListOf<PageNode>()
        val orphans = mutableListOf<MarkdownDocument>()

        // Create root node
        val rootDoc = documents.find { it.relativePath.nameCount == 1 && it.isIndex }
        val root = PageNode(
            id = "",
            title = rootDoc?.title ?: rootTitle,
            path = Path(""),
            document = rootDoc,
            weight = rootDoc?.frontmatter?.weight ?: 0
        )
        nodesByPath[Path("")] = root
        nodesById[""] = root

        // Group documents by directory
        val docsByDir = documents
            .filter { it != rootDoc }
            .groupBy { it.relativePath.parent ?: Path("") }

        // Pre-create directory nodes for all directories that have index.md files
        // This prevents duplicate nodes when processing documents
        // Sort by depth (shallowest first) so parent directories are created before children
        val indexDocs = documents
            .filter { it.isIndex && it != rootDoc }
            .sortedBy { it.relativePath.nameCount }
        logger.info("Found ${indexDocs.size} index.md files to pre-create")
        for (doc in indexDocs) {
            val dirPath = doc.relativePath.parent ?: Path("")
            logger.info("Processing index.md: ${doc.relativePath} -> dirPath: $dirPath")
            if (!nodesByPath.containsKey(dirPath)) {
                // Create node for this directory using its index.md
                val node = PageNode(
                    id = dirPath.toString(),
                    title = doc.title,
                    path = dirPath,
                    document = doc,
                    weight = doc.frontmatter.weight ?: 0
                )

                // Ensure parent of this directory exists
                val parentPath = dirPath.parent ?: Path("")
                ensureParentChain(parentPath, nodesByPath, nodesById, virtualNodes, documents)

                // Add to parent
                val parent = nodesByPath[parentPath] ?: root
                parent.addChild(node)

                nodesByPath[dirPath] = node
                nodesByPath[doc.relativePath] = node  // Map both dir and index.md to same node
                nodesById[node.id] = node

                logger.info("Pre-created directory node for: $dirPath from ${doc.relativePath}")
            } else {
                logger.info("Directory node already exists for: $dirPath")
            }
        }

        // Process documents, creating directory structure as needed
        for (doc in documents.filter { it != rootDoc }) {
            try {
                val parentPath = getParentPath(doc)

                // Ensure parent chain exists (this may create a node for this doc's directory)
                ensureParentChain(parentPath, nodesByPath, nodesById, virtualNodes, documents)

                // Skip index.md files that were already processed in the pre-creation phase
                if (doc.isIndex && nodesByPath.containsKey(doc.relativePath)) {
                    logger.debug("Skipping already-processed index.md: ${doc.relativePath}")
                    continue
                }

                val node = createNodeForDocument(doc)

                // Add to parent
                val parent = nodesByPath[parentPath] ?: root
                parent.addChild(node)

                nodesByPath[doc.relativePath] = node
                nodesById[node.id] = node

            } catch (e: Exception) {
                logger.error("Failed to add document to hierarchy: ${doc.relativePath}", e)
                orphans.add(doc)
            }
        }

        // Sort all children
        root.sortChildren()

        logger.info("Built hierarchy: ${nodesById.size} nodes, ${virtualNodes.size} virtual, ${orphans.size} orphans")

        return HierarchyResult(
            root = root,
            nodesByPath = nodesByPath,
            nodesById = nodesById,
            orphans = orphans,
            virtualNodes = virtualNodes
        )
    }

    /**
     * Create a PageNode from a MarkdownDocument.
     */
    private fun createNodeForDocument(doc: MarkdownDocument): PageNode {
        return PageNode(
            id = doc.relativePath.toString(),
            title = doc.title,
            path = doc.relativePath,
            document = doc,
            weight = doc.frontmatter.weight ?: 0,
            confluenceId = doc.frontmatter.confluenceId
        )
    }

    /**
     * Get the parent path for a document.
     *
     * For index.md files, the parent is the grandparent directory.
     * For regular files, the parent is the containing directory.
     */
    private fun getParentPath(doc: MarkdownDocument): Path {
        val dirPath = doc.relativePath.parent ?: Path("")

        return if (doc.isIndex) {
            // index.md's parent is the grandparent directory
            dirPath.parent ?: Path("")
        } else {
            // Regular file's parent is its directory (or index.md in that directory)
            dirPath
        }
    }

    /**
     * Ensure all directories in the path exist as nodes.
     * Creates virtual nodes for directories without index.md.
     */
    private fun ensureParentChain(
        path: Path,
        nodesByPath: MutableMap<Path, PageNode>,
        nodesById: MutableMap<String, PageNode>,
        virtualNodes: MutableList<PageNode>,
        documents: List<MarkdownDocument>
    ) {
        if (path.toString().isEmpty() || nodesByPath.containsKey(path)) {
            return
        }

        // Ensure grandparent exists first
        val parentPath = path.parent ?: Path("")
        ensureParentChain(parentPath, nodesByPath, nodesById, virtualNodes, documents)

        // Check if there's an index.md for this directory
        val indexPath = path.resolve(indexFileName)
        val indexDoc = documents.find { it.relativePath == indexPath }

        val node = if (indexDoc != null) {
            // Directory has an index.md - it will be added as a regular document
            // Just create a placeholder that will be replaced
            PageNode(
                id = path.toString(),
                title = indexDoc.title,
                path = path,
                document = indexDoc,
                weight = indexDoc.frontmatter.weight ?: 0
            )
        } else {
            // Create virtual node for directory
            val title = formatDirectoryTitle(path.fileName?.toString() ?: "Unknown")
            val virtual = PageNode(
                id = path.toString(),
                title = title,
                path = path,
                document = null,
                isVirtual = true
            )
            virtualNodes.add(virtual)
            virtual
        }

        // Add to parent
        val parent = nodesByPath[parentPath]
        parent?.addChild(node)

        nodesByPath[path] = node
        nodesById[node.id] = node
    }

    /**
     * Format a directory name as a title.
     */
    private fun formatDirectoryTitle(name: String): String {
        return name
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    companion object {
        /**
         * Create a builder with default settings.
         */
        fun default(): HierarchyBuilder = HierarchyBuilder()
    }
}
