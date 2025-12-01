package com.consync.util

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        EnvLoader.clear()
    }

    @AfterEach
    fun cleanup() {
        EnvLoader.clear()
    }

    @Test
    fun `should load simple key-value pairs`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("""
            KEY1=value1
            KEY2=value2
        """.trimIndent())

        val loaded = EnvLoader.load(envFile)

        assertEquals(2, loaded.size)
        assertEquals("value1", EnvLoader.get("KEY1"))
        assertEquals("value2", EnvLoader.get("KEY2"))
    }

    @Test
    fun `should handle quoted values`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("""
            DOUBLE_QUOTED="value with spaces"
            SINGLE_QUOTED='another value'
        """.trimIndent())

        EnvLoader.load(envFile)

        assertEquals("value with spaces", EnvLoader.get("DOUBLE_QUOTED"))
        assertEquals("another value", EnvLoader.get("SINGLE_QUOTED"))
    }

    @Test
    fun `should skip comments and empty lines`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("""
            # This is a comment
            KEY1=value1

            # Another comment
            KEY2=value2

        """.trimIndent())

        val loaded = EnvLoader.load(envFile)

        assertEquals(2, loaded.size)
        assertEquals("value1", EnvLoader.get("KEY1"))
        assertEquals("value2", EnvLoader.get("KEY2"))
    }

    @Test
    fun `should handle escape sequences`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("""
            NEWLINE="line1\nline2"
            TAB="col1\tcol2"
        """.trimIndent())

        EnvLoader.load(envFile)

        assertEquals("line1\nline2", EnvLoader.get("NEWLINE"))
        assertEquals("col1\tcol2", EnvLoader.get("TAB"))
    }

    @Test
    fun `should return null for non-existent keys`() {
        assertNull(EnvLoader.get("NON_EXISTENT_KEY"))
    }

    @Test
    fun `should return default value for non-existent keys`() {
        assertEquals("default", EnvLoader.get("NON_EXISTENT_KEY", "default"))
    }

    @Test
    fun `should return empty map for non-existent file`() {
        val loaded = EnvLoader.load(tempDir.resolve("non-existent.env"))

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun `should clear loaded variables`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("KEY=value")

        EnvLoader.load(envFile)
        assertEquals("value", EnvLoader.get("KEY"))

        EnvLoader.clear()
        assertNull(EnvLoader.get("KEY"))
    }

    @Test
    fun `should get all loaded variables`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("""
            KEY1=value1
            KEY2=value2
            KEY3=value3
        """.trimIndent())

        EnvLoader.load(envFile)

        val all = EnvLoader.getAll()
        assertEquals(3, all.size)
        assertEquals("value1", all["KEY1"])
        assertEquals("value2", all["KEY2"])
        assertEquals("value3", all["KEY3"])
    }

    @Test
    fun `should handle values with equals signs`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("URL=https://example.com?foo=bar&baz=qux")

        EnvLoader.load(envFile)

        assertEquals("https://example.com?foo=bar&baz=qux", EnvLoader.get("URL"))
    }

    @Test
    fun `should handle empty values`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("""
            EMPTY=
            QUOTED_EMPTY=""
        """.trimIndent())

        EnvLoader.load(envFile)

        assertEquals("", EnvLoader.get("EMPTY"))
        assertEquals("", EnvLoader.get("QUOTED_EMPTY"))
    }

    @Test
    fun `should expand env vars in string`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("""
            BASE_URL=https://example.com
            API_KEY=secret123
        """.trimIndent())

        EnvLoader.load(envFile)

        val template = "Connect to \${BASE_URL} with key \${API_KEY}"
        val expanded = template.expandEnvVars()

        assertEquals("Connect to https://example.com with key secret123", expanded)
    }

    @Test
    fun `should keep unexpanded vars if not found`() {
        EnvLoader.clear()

        val template = "Value: \${NON_EXISTENT_VAR}"
        val expanded = template.expandEnvVars()

        assertEquals("Value: \${NON_EXISTENT_VAR}", expanded)
    }

    @Test
    fun `should load from multiple potential locations`() {
        val env1 = tempDir.resolve("env1")
        val env2 = tempDir.resolve("env2")

        // Only create env2
        env2.writeText("KEY=from-env2")

        val loaded = EnvLoader.loadFromLocations(env1, env2)

        assertEquals(1, loaded.size)
        assertEquals("from-env2", EnvLoader.get("KEY"))
    }

    @Test
    fun `should not override existing variables by default`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("KEY=original")

        // Load first time
        EnvLoader.load(envFile)
        assertEquals("original", EnvLoader.get("KEY"))

        // Try to load again with different value
        val envFile2 = tempDir.resolve(".env2")
        envFile2.writeText("KEY=new-value")

        EnvLoader.load(envFile2, override = false)
        assertEquals("original", EnvLoader.get("KEY"))
    }

    @Test
    fun `should override existing variables when requested`() {
        val envFile = tempDir.resolve(".env")
        envFile.writeText("KEY=original")

        EnvLoader.load(envFile)
        assertEquals("original", EnvLoader.get("KEY"))

        // Load with override
        val envFile2 = tempDir.resolve(".env2")
        envFile2.writeText("KEY=new-value")

        EnvLoader.load(envFile2, override = true)
        assertEquals("new-value", EnvLoader.get("KEY"))
    }
}
