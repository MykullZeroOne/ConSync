package com.consync.core.hierarchy

import com.consync.core.markdown.MarkdownDocument
import java.nio.file.Path

/**
 * Represents a node in the page hierarchy tree.
 *
 * Each node corresponds to either a markdown document or a virtual
 * directory node (for directories without an index.md).
 */
data class PageNode(
    /** Unique identifier for this node (relative path) */
    val id: String,

    /** Page title */
    val title: String,

    /** Path relative to documentation root */
    val path: Path,

    /** The source markdown document (null for virtual nodes) */
    val document: MarkdownDocument?,

    /** Parent node (null for root) */
    var parent: PageNode? = null,

    /** Child nodes */
    val children: MutableList<PageNode> = mutableListOf(),

    /** Confluence page ID if synced */
    var confluenceId: String? = null,

    /** Order/weight for sorting */
    val weight: Int = 0,

    /** Whether this is a virtual directory node */
    val isVirtual: Boolean = false
) {
    /** Check if this is a root node */
    val isRoot: Boolean
        get() = parent == null

    /** Check if this is a leaf node (no children) */
    val isLeaf: Boolean
        get() = children.isEmpty()

    /** Get depth in the tree (root = 0) */
    val depth: Int
        get() = if (parent == null) 0 else parent!!.depth + 1

    /** Get all ancestors from root to parent */
    val ancestors: List<PageNode>
        get() {
            val result = mutableListOf<PageNode>()
            var current = parent
            while (current != null) {
                result.add(0, current)
                current = current.parent
            }
            return result
        }

    /** Get all descendants (children, grandchildren, etc.) */
    val descendants: List<PageNode>
        get() {
            val result = mutableListOf<PageNode>()
            for (child in children) {
                result.add(child)
                result.addAll(child.descendants)
            }
            return result
        }

    /** Get path from root as list of nodes */
    val pathFromRoot: List<PageNode>
        get() = ancestors + this

    /** Get sibling nodes (nodes with same parent) */
    val siblings: List<PageNode>
        get() = parent?.children?.filter { it != this } ?: emptyList()

    /**
     * Add a child node.
     */
    fun addChild(child: PageNode) {
        child.parent = this
        children.add(child)
    }

    /**
     * Remove a child node.
     */
    fun removeChild(child: PageNode): Boolean {
        if (children.remove(child)) {
            child.parent = null
            return true
        }
        return false
    }

    /**
     * Find a descendant by path.
     */
    fun findByPath(targetPath: Path): PageNode? {
        if (path == targetPath) return this
        for (child in children) {
            val found = child.findByPath(targetPath)
            if (found != null) return found
        }
        return null
    }

    /**
     * Find a descendant by ID.
     */
    fun findById(targetId: String): PageNode? {
        if (id == targetId) return this
        for (child in children) {
            val found = child.findById(targetId)
            if (found != null) return found
        }
        return null
    }

    /**
     * Sort children by weight, then by title.
     */
    fun sortChildren() {
        children.sortWith(compareBy({ it.weight }, { it.title.lowercase() }))
        children.forEach { it.sortChildren() }
    }

    /**
     * Get a flat list of all nodes in tree order (pre-order traversal).
     */
    fun flatten(): List<PageNode> {
        val result = mutableListOf<PageNode>()
        result.add(this)
        for (child in children) {
            result.addAll(child.flatten())
        }
        return result
    }

    /**
     * Print tree structure for debugging.
     */
    fun printTree(indent: String = ""): String {
        val sb = StringBuilder()
        val marker = if (isVirtual) "[V]" else ""
        sb.appendLine("$indent$title $marker($id)")
        for (child in children) {
            sb.append(child.printTree("$indent  "))
        }
        return sb.toString()
    }

    override fun toString(): String = "PageNode(id=$id, title=$title, children=${children.size})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PageNode) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Result of building a page hierarchy.
 */
data class HierarchyResult(
    /** Root node of the tree */
    val root: PageNode,

    /** All nodes indexed by path */
    val nodesByPath: Map<Path, PageNode>,

    /** All nodes indexed by ID */
    val nodesById: Map<String, PageNode>,

    /** Documents that couldn't be placed in hierarchy */
    val orphans: List<MarkdownDocument>,

    /** Virtual nodes created for directories */
    val virtualNodes: List<PageNode>
) {
    /** Total number of nodes */
    val totalNodes: Int
        get() = nodesById.size

    /** Total number of real (non-virtual) nodes */
    val realNodes: Int
        get() = totalNodes - virtualNodes.size

    /** Maximum depth of the tree */
    val maxDepth: Int
        get() = root.flatten().maxOfOrNull { it.depth } ?: 0

    /**
     * Get node by path.
     */
    fun getByPath(path: Path): PageNode? = nodesByPath[path]

    /**
     * Get node by ID.
     */
    fun getById(id: String): PageNode? = nodesById[id]

    /**
     * Print the entire tree structure.
     */
    fun printTree(): String = root.printTree()
}

/**
 * Represents a link between two pages in the hierarchy.
 */
data class PageLink(
    /** Source page node */
    val source: PageNode,

    /** Target page node */
    val target: PageNode,

    /** Link text from source document */
    val linkText: String,

    /** Original href from markdown */
    val originalHref: String
)
