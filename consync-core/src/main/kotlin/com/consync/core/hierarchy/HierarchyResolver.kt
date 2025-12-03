package com.consync.core.hierarchy

import com.consync.core.markdown.DocumentLink
import com.consync.core.markdown.MarkdownDocument
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Resolves relationships between pages in the hierarchy.
 *
 * Handles:
 * - Internal link resolution to page nodes
 * - Finding common ancestors
 * - Computing relative paths between pages
 * - Validating hierarchy consistency
 */
class HierarchyResolver {
    private val logger = LoggerFactory.getLogger(HierarchyResolver::class.java)

    /**
     * Resolve all internal links to their target page nodes.
     *
     * @param hierarchy The built hierarchy
     * @return List of resolved page links
     */
    fun resolveLinks(hierarchy: HierarchyResult): List<PageLink> {
        val links = mutableListOf<PageLink>()

        for (node in hierarchy.root.flatten()) {
            if (node.document == null) continue

            for (docLink in node.document.links) {
                if (!docLink.isInternal || docLink.resolvedPath == null) continue

                val targetNode = findTargetNode(docLink.resolvedPath, hierarchy)
                if (targetNode != null) {
                    links.add(PageLink(
                        source = node,
                        target = targetNode,
                        linkText = docLink.text,
                        originalHref = docLink.href
                    ))
                } else {
                    logger.debug("Unresolved link in ${node.path}: ${docLink.href}")
                }
            }
        }

        logger.info("Resolved ${links.size} internal links")
        return links
    }

    /**
     * Find the target node for a link path.
     */
    private fun findTargetNode(linkPath: Path, hierarchy: HierarchyResult): PageNode? {
        // Try exact path match
        hierarchy.nodesByPath[linkPath]?.let { return it }

        // Try with .md extension
        val withMd = Path.of(linkPath.toString() + ".md")
        hierarchy.nodesByPath[withMd]?.let { return it }

        // Try as directory with index.md
        val asIndex = linkPath.resolve("index.md")
        hierarchy.nodesByPath[asIndex]?.let { return it }

        // Try without extension if it has one
        val pathStr = linkPath.toString()
        if (pathStr.endsWith(".md")) {
            val withoutExt = Path.of(pathStr.removeSuffix(".md"))
            hierarchy.nodesByPath[withoutExt]?.let { return it }
        }

        return null
    }

    /**
     * Find the nearest common ancestor of two nodes.
     *
     * @param node1 First node
     * @param node2 Second node
     * @return Common ancestor, or null if no common ancestor
     */
    fun findCommonAncestor(node1: PageNode, node2: PageNode): PageNode? {
        val ancestors1 = node1.pathFromRoot.toSet()
        for (ancestor in node2.pathFromRoot.reversed()) {
            if (ancestor in ancestors1) {
                return ancestor
            }
        }
        return null
    }

    /**
     * Compute relative path from source to target in the hierarchy.
     *
     * @param source Source node
     * @param target Target node
     * @return Relative path string (e.g., "../sibling" or "child/grandchild")
     */
    fun computeRelativePath(source: PageNode, target: PageNode): String {
        if (source == target) return ""

        val commonAncestor = findCommonAncestor(source, target)

        // Steps up from source to common ancestor
        val stepsUp = if (commonAncestor != null) {
            source.depth - commonAncestor.depth
        } else {
            source.depth
        }

        // Path down from common ancestor to target
        val pathDown = if (commonAncestor != null) {
            target.pathFromRoot.drop(commonAncestor.depth + 1)
        } else {
            target.pathFromRoot
        }

        val upPart = "../".repeat(stepsUp)
        val downPart = pathDown.joinToString("/") { it.path.fileName?.toString() ?: "" }

        return (upPart + downPart).ifEmpty { "." }
    }

    /**
     * Validate hierarchy consistency.
     *
     * Checks:
     * - All nodes have valid parent references
     * - No circular references
     * - All documents are accounted for
     *
     * @param hierarchy The hierarchy to validate
     * @return List of validation errors (empty if valid)
     */
    fun validate(hierarchy: HierarchyResult): List<String> {
        val errors = mutableListOf<String>()

        // Check for orphan nodes (not reachable from root)
        val reachable = hierarchy.root.flatten().toSet()
        for ((path, node) in hierarchy.nodesByPath) {
            if (node !in reachable) {
                errors.add("Node not reachable from root: $path")
            }
        }

        // Check parent-child consistency
        for (node in hierarchy.root.flatten()) {
            for (child in node.children) {
                if (child.parent != node) {
                    errors.add("Inconsistent parent reference: ${child.id} parent should be ${node.id}")
                }
            }
        }

        // Check for duplicate IDs
        val ids = mutableSetOf<String>()
        for (node in hierarchy.root.flatten()) {
            if (node.id in ids) {
                errors.add("Duplicate node ID: ${node.id}")
            }
            ids.add(node.id)
        }

        if (errors.isEmpty()) {
            logger.info("Hierarchy validation passed")
        } else {
            logger.warn("Hierarchy validation found ${errors.size} errors")
        }

        return errors
    }

    /**
     * Get all nodes at a specific depth.
     *
     * @param hierarchy The hierarchy
     * @param depth Target depth (0 = root)
     * @return Nodes at that depth
     */
    fun getNodesAtDepth(hierarchy: HierarchyResult, depth: Int): List<PageNode> {
        return hierarchy.root.flatten().filter { it.depth == depth }
    }

    /**
     * Get all leaf nodes (nodes without children).
     */
    fun getLeafNodes(hierarchy: HierarchyResult): List<PageNode> {
        return hierarchy.root.flatten().filter { it.isLeaf }
    }

    /**
     * Get all branch nodes (nodes with children).
     */
    fun getBranchNodes(hierarchy: HierarchyResult): List<PageNode> {
        return hierarchy.root.flatten().filter { !it.isLeaf }
    }

    /**
     * Find broken links (links to non-existent pages).
     *
     * @param hierarchy The hierarchy
     * @return List of broken links with source document and href
     */
    fun findBrokenLinks(hierarchy: HierarchyResult): List<BrokenLink> {
        val brokenLinks = mutableListOf<BrokenLink>()

        for (node in hierarchy.root.flatten()) {
            if (node.document == null) continue

            for (docLink in node.document.links) {
                if (!docLink.isInternal || docLink.resolvedPath == null) continue

                val targetNode = findTargetNode(docLink.resolvedPath, hierarchy)
                if (targetNode == null) {
                    brokenLinks.add(BrokenLink(
                        sourceNode = node,
                        href = docLink.href,
                        resolvedPath = docLink.resolvedPath,
                        lineNumber = docLink.lineNumber
                    ))
                }
            }
        }

        if (brokenLinks.isNotEmpty()) {
            logger.warn("Found ${brokenLinks.size} broken links")
        }

        return brokenLinks
    }

    /**
     * Build a map of which pages link to each page (backlinks).
     *
     * @param hierarchy The hierarchy
     * @return Map of target node to list of source nodes that link to it
     */
    fun buildBacklinks(hierarchy: HierarchyResult): Map<PageNode, List<PageNode>> {
        val backlinks = mutableMapOf<PageNode, MutableList<PageNode>>()

        val links = resolveLinks(hierarchy)
        for (link in links) {
            backlinks.getOrPut(link.target) { mutableListOf() }.add(link.source)
        }

        return backlinks
    }

    /**
     * Get statistics about the hierarchy.
     */
    fun getStatistics(hierarchy: HierarchyResult): HierarchyStatistics {
        val allNodes = hierarchy.root.flatten()
        val depths = allNodes.map { it.depth }

        return HierarchyStatistics(
            totalNodes = allNodes.size,
            realNodes = allNodes.count { !it.isVirtual },
            virtualNodes = allNodes.count { it.isVirtual },
            leafNodes = allNodes.count { it.isLeaf },
            branchNodes = allNodes.count { !it.isLeaf },
            maxDepth = depths.maxOrNull() ?: 0,
            avgDepth = if (depths.isNotEmpty()) depths.average() else 0.0,
            orphanDocuments = hierarchy.orphans.size
        )
    }
}

/**
 * Represents a broken link in the hierarchy.
 */
data class BrokenLink(
    val sourceNode: PageNode,
    val href: String,
    val resolvedPath: Path,
    val lineNumber: Int
)

/**
 * Statistics about a hierarchy.
 */
data class HierarchyStatistics(
    val totalNodes: Int,
    val realNodes: Int,
    val virtualNodes: Int,
    val leafNodes: Int,
    val branchNodes: Int,
    val maxDepth: Int,
    val avgDepth: Double,
    val orphanDocuments: Int
)
