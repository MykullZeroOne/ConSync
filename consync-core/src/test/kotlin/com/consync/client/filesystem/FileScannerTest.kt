package com.consync.client.filesystem

import com.consync.config.FilesConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileScannerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should find all markdown files`() {
        // Create test files
        tempDir.resolve("file1.md").writeText("# File 1")
        tempDir.resolve("file2.md").writeText("# File 2")
        tempDir.resolve("file3.txt").writeText("Not markdown")

        val scanner = FileScanner()
        val files = scanner.scan(tempDir)

        assertEquals(2, files.size)
        assertTrue(files.any { it.toString() == "file1.md" })
        assertTrue(files.any { it.toString() == "file2.md" })
    }

    @Test
    fun `should find files in subdirectories`() {
        // Create directory structure
        val subDir = tempDir.resolve("sub")
        subDir.createDirectories()

        tempDir.resolve("root.md").createFile()
        subDir.resolve("nested.md").createFile()

        val scanner = FileScanner()
        val files = scanner.scan(tempDir)

        assertEquals(2, files.size)
        assertTrue(files.any { it.toString() == "root.md" })
        assertTrue(files.any { it.toString().contains("nested.md") })
    }

    @Test
    fun `should respect include patterns`() {
        tempDir.resolve("doc.md").createFile()
        tempDir.resolve("doc.markdown").createFile()
        tempDir.resolve("doc.txt").createFile()

        val config = FilesConfig(
            include = listOf("**/*.md", "**/*.markdown")
        )
        val scanner = FileScanner(config)
        val files = scanner.scan(tempDir)

        assertEquals(2, files.size)
    }

    @Test
    fun `should respect exclude patterns`() {
        // Create files
        tempDir.resolve("include.md").createFile()
        val nodeModules = tempDir.resolve("node_modules")
        nodeModules.createDirectories()
        nodeModules.resolve("package.md").createFile()

        val config = FilesConfig(
            include = listOf("**/*.md"),
            exclude = listOf("**/node_modules/**")
        )
        val scanner = FileScanner(config)
        val files = scanner.scan(tempDir)

        assertEquals(1, files.size)
        assertEquals("include.md", files[0].toString())
    }

    @Test
    fun `should skip hidden directories`() {
        // Create hidden directory
        val hiddenDir = tempDir.resolve(".hidden")
        hiddenDir.createDirectories()
        hiddenDir.resolve("secret.md").createFile()

        tempDir.resolve("visible.md").createFile()

        val scanner = FileScanner()
        val files = scanner.scan(tempDir)

        assertEquals(1, files.size)
        assertEquals("visible.md", files[0].toString())
    }

    @Test
    fun `should throw for non-existent directory`() {
        val nonExistent = tempDir.resolve("does-not-exist")
        val scanner = FileScanner()

        assertThrows<IllegalArgumentException> {
            scanner.scan(nonExistent)
        }
    }

    @Test
    fun `should throw for file path instead of directory`() {
        val file = tempDir.resolve("file.md")
        file.createFile()

        val scanner = FileScanner()

        assertThrows<IllegalArgumentException> {
            scanner.scan(file)
        }
    }

    @Test
    fun `should read file content`() {
        val content = "# Hello World\n\nSome content here."
        tempDir.resolve("test.md").writeText(content)

        val scanner = FileScanner()
        val readContent = scanner.readFile(tempDir, Path.of("test.md"))

        assertEquals(content, readContent)
    }

    @Test
    fun `should check file existence`() {
        tempDir.resolve("exists.md").createFile()

        val scanner = FileScanner()

        assertTrue(scanner.exists(tempDir, Path.of("exists.md")))
        assertFalse(scanner.exists(tempDir, Path.of("missing.md")))
    }

    @Test
    fun `should get last modified time`() {
        tempDir.resolve("test.md").createFile()

        val scanner = FileScanner()
        val lastModified = scanner.getLastModified(tempDir, Path.of("test.md"))

        // Should be recent
        val now = java.time.Instant.now()
        assertTrue(lastModified.isBefore(now.plusSeconds(1)))
        assertTrue(lastModified.isAfter(now.minusSeconds(60)))
    }

    @Test
    fun `should return sorted file list`() {
        tempDir.resolve("zebra.md").createFile()
        tempDir.resolve("apple.md").createFile()
        tempDir.resolve("mango.md").createFile()

        val scanner = FileScanner()
        val files = scanner.scan(tempDir)

        assertEquals(listOf("apple.md", "mango.md", "zebra.md"), files.map { it.toString() })
    }

    @Test
    fun `should handle complex exclude patterns`() {
        // Create structure
        tempDir.resolve("docs/guide.md").also {
            it.parent.createDirectories()
            it.createFile()
        }
        tempDir.resolve("docs/_drafts/draft.md").also {
            it.parent.createDirectories()
            it.createFile()
        }
        tempDir.resolve("README.md").createFile()

        val config = FilesConfig(
            include = listOf("**/*.md"),
            exclude = listOf("**/_*/**", "README.md")
        )
        val scanner = FileScanner(config)
        val files = scanner.scan(tempDir)

        assertEquals(1, files.size)
        assertTrue(files[0].toString().contains("guide.md"))
    }

    @Test
    fun `should handle empty directory`() {
        val emptyDir = tempDir.resolve("empty")
        emptyDir.createDirectories()

        val scanner = FileScanner()
        val files = scanner.scan(emptyDir)

        assertTrue(files.isEmpty())
    }

    @Test
    fun `default config should exclude common directories`() {
        // Create typical project structure
        tempDir.resolve("doc.md").createFile()

        val nodeModules = tempDir.resolve("node_modules/package")
        nodeModules.createDirectories()
        nodeModules.resolve("readme.md").createFile()

        val gitDir = tempDir.resolve(".git/objects")
        gitDir.createDirectories()
        gitDir.resolve("info.md").createFile()

        val scanner = FileScanner(FileScanner.DEFAULT_CONFIG)
        val files = scanner.scan(tempDir)

        assertEquals(1, files.size)
        assertEquals("doc.md", files[0].toString())
    }
}
