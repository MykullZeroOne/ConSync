package com.consync.core.hierarchy

import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.*

class PageNodeTest {

    private fun createNode(
        id: String,
        title: String = id,
        path: String = id,
        isVirtual: Boolean = false
    ) = PageNode(
        id = id,
        title = title,
        path = Path.of(path),
        document = null,
        isVirtual = isVirtual
    )

    @Test
    fun `should identify root node`() {
        val root = createNode("root")
        val child = createNode("child")
        root.addChild(child)

        assertTrue(root.isRoot)
        assertFalse(child.isRoot)
    }

    @Test
    fun `should identify leaf nodes`() {
        val parent = createNode("parent")
        val child = createNode("child")
        parent.addChild(child)

        assertFalse(parent.isLeaf)
        assertTrue(child.isLeaf)
    }

    @Test
    fun `should calculate depth correctly`() {
        val root = createNode("root")
        val level1 = createNode("level1")
        val level2 = createNode("level2")

        root.addChild(level1)
        level1.addChild(level2)

        assertEquals(0, root.depth)
        assertEquals(1, level1.depth)
        assertEquals(2, level2.depth)
    }

    @Test
    fun `should get ancestors`() {
        val root = createNode("root")
        val parent = createNode("parent")
        val child = createNode("child")

        root.addChild(parent)
        parent.addChild(child)

        val ancestors = child.ancestors
        assertEquals(2, ancestors.size)
        assertEquals(root, ancestors[0])
        assertEquals(parent, ancestors[1])
    }

    @Test
    fun `should get descendants`() {
        val root = createNode("root")
        val child1 = createNode("child1")
        val child2 = createNode("child2")
        val grandchild = createNode("grandchild")

        root.addChild(child1)
        root.addChild(child2)
        child1.addChild(grandchild)

        val descendants = root.descendants
        assertEquals(3, descendants.size)
        assertTrue(child1 in descendants)
        assertTrue(child2 in descendants)
        assertTrue(grandchild in descendants)
    }

    @Test
    fun `should get path from root`() {
        val root = createNode("root")
        val parent = createNode("parent")
        val child = createNode("child")

        root.addChild(parent)
        parent.addChild(child)

        val path = child.pathFromRoot
        assertEquals(3, path.size)
        assertEquals(listOf(root, parent, child), path)
    }

    @Test
    fun `should get siblings`() {
        val parent = createNode("parent")
        val child1 = createNode("child1")
        val child2 = createNode("child2")
        val child3 = createNode("child3")

        parent.addChild(child1)
        parent.addChild(child2)
        parent.addChild(child3)

        val siblings = child1.siblings
        assertEquals(2, siblings.size)
        assertTrue(child2 in siblings)
        assertTrue(child3 in siblings)
        assertFalse(child1 in siblings)
    }

    @Test
    fun `should add and remove children`() {
        val parent = createNode("parent")
        val child = createNode("child")

        parent.addChild(child)
        assertEquals(1, parent.children.size)
        assertEquals(parent, child.parent)

        val removed = parent.removeChild(child)
        assertTrue(removed)
        assertEquals(0, parent.children.size)
        assertNull(child.parent)
    }

    @Test
    fun `should find node by path`() {
        val root = createNode("root", path = "")
        val docs = createNode("docs", path = "docs")
        val guide = createNode("guide", path = "docs/guide.md")

        root.addChild(docs)
        docs.addChild(guide)

        assertEquals(root, root.findByPath(Path.of("")))
        assertEquals(docs, root.findByPath(Path.of("docs")))
        assertEquals(guide, root.findByPath(Path.of("docs/guide.md")))
        assertNull(root.findByPath(Path.of("nonexistent")))
    }

    @Test
    fun `should find node by id`() {
        val root = createNode("root")
        val child = createNode("child-123")

        root.addChild(child)

        assertEquals(child, root.findById("child-123"))
        assertNull(root.findById("nonexistent"))
    }

    @Test
    fun `should sort children by weight then title`() {
        val parent = createNode("parent")
        val z = PageNode("z", "Zebra", Path.of("z"), null, weight = 0)
        val a = PageNode("a", "Apple", Path.of("a"), null, weight = 0)
        val first = PageNode("first", "First", Path.of("first"), null, weight = -10)

        parent.addChild(z)
        parent.addChild(a)
        parent.addChild(first)

        parent.sortChildren()

        assertEquals("First", parent.children[0].title)  // weight -10
        assertEquals("Apple", parent.children[1].title)  // weight 0, "Apple" < "Zebra"
        assertEquals("Zebra", parent.children[2].title)  // weight 0
    }

    @Test
    fun `should flatten tree in pre-order`() {
        val root = createNode("root")
        val a = createNode("a")
        val b = createNode("b")
        val a1 = createNode("a1")

        root.addChild(a)
        root.addChild(b)
        a.addChild(a1)

        val flat = root.flatten()
        assertEquals(listOf("root", "a", "a1", "b"), flat.map { it.id })
    }

    @Test
    fun `should generate tree string`() {
        val root = createNode("root", title = "Root")
        val child = createNode("child", title = "Child")
        root.addChild(child)

        val tree = root.printTree()
        assertTrue(tree.contains("Root"))
        assertTrue(tree.contains("Child"))
    }

    @Test
    fun `should handle virtual nodes`() {
        val virtual = createNode("virtual", isVirtual = true)
        val real = createNode("real", isVirtual = false)

        assertTrue(virtual.isVirtual)
        assertFalse(real.isVirtual)
    }

    @Test
    fun `should use equals and hashCode by id`() {
        val node1 = createNode("same-id", title = "Title 1")
        val node2 = createNode("same-id", title = "Title 2")
        val node3 = createNode("different-id")

        assertEquals(node1, node2)
        assertNotEquals(node1, node3)
        assertEquals(node1.hashCode(), node2.hashCode())
    }
}
