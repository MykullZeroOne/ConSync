package com.consync.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigLoaderTest {

    private val loader = ConfigLoader()

    @Test
    fun `should parse minimal valid configuration`() {
        val yaml = """
            confluence:
              base_url: "https://example.atlassian.net/wiki"
              username: "user@example.com"
              api_token: "token123"
            space:
              key: "DOCS"
        """.trimIndent()

        val config = loader.parse(yaml)

        assertEquals("https://example.atlassian.net/wiki", config.confluence.baseUrl)
        assertEquals("user@example.com", config.confluence.username)
        assertEquals("token123", config.confluence.apiToken)
        assertEquals("DOCS", config.space.key)
    }

    @Test
    fun `should parse full configuration`() {
        val yaml = """
            confluence:
              base_url: "https://example.atlassian.net/wiki"
              username: "user@example.com"
              api_token: "token123"
              timeout: 60
              retry_count: 5
            space:
              key: "DOCS"
              name: "Documentation"
              root_page_title: "Home"
            content:
              title_source: frontmatter
              toc:
                enabled: true
                depth: 4
                position: bottom
            sync:
              delete_orphans: true
              preserve_labels: false
            files:
              include:
                - "**/*.md"
                - "**/*.markdown"
              exclude:
                - "**/draft/**"
        """.trimIndent()

        val config = loader.parse(yaml)

        assertEquals(60, config.confluence.timeout)
        assertEquals(5, config.confluence.retryCount)
        assertEquals("Documentation", config.space.name)
        assertEquals("Home", config.space.rootPageTitle)
        assertEquals(TitleSource.FRONTMATTER, config.content.titleSource)
        assertEquals(4, config.content.toc.depth)
        assertEquals(TocPosition.BOTTOM, config.content.toc.position)
        assertTrue(config.sync.deleteOrphans)
        assertEquals(2, config.files.include.size)
        assertEquals(1, config.files.exclude.size)
    }

    @Test
    fun `should use defaults for optional fields`() {
        val yaml = """
            confluence:
              base_url: "https://example.atlassian.net/wiki"
              pat: "pat-token"
            space:
              key: "TEST"
        """.trimIndent()

        val config = loader.parse(yaml)

        // Check defaults
        assertEquals(30, config.confluence.timeout)
        assertEquals(3, config.confluence.retryCount)
        assertEquals(TitleSource.FILENAME, config.content.titleSource)
        assertTrue(config.content.toc.enabled)
        assertEquals(3, config.content.toc.depth)
        assertEquals(TocPosition.TOP, config.content.toc.position)
        assertEquals(false, config.sync.deleteOrphans)
        assertTrue(config.sync.preserveLabels)
        assertEquals(listOf("**/*.md"), config.files.include)
        assertTrue(config.files.exclude.isEmpty())
    }

    @Test
    fun `should throw exception for invalid yaml`() {
        val invalidYaml = """
            confluence:
              base_url: [invalid
        """.trimIndent()

        assertThrows<ConfigLoadException> {
            loader.parse(invalidYaml)
        }
    }

    @Test
    fun `should throw exception for missing required fields`() {
        val yaml = """
            confluence:
              base_url: "https://example.atlassian.net/wiki"
        """.trimIndent()

        // This should throw because space.key is required
        assertThrows<ConfigLoadException> {
            loader.parse(yaml)
        }
    }

    @Test
    fun `should parse PAT authentication`() {
        val yaml = """
            confluence:
              base_url: "https://confluence.company.com"
              pat: "personal-access-token"
            space:
              key: "PROJ"
        """.trimIndent()

        val config = loader.parse(yaml)

        assertEquals("personal-access-token", config.confluence.pat)
        assertEquals(null, config.confluence.username)
        assertEquals(null, config.confluence.apiToken)
    }

    @Test
    fun `should parse all enum values`() {
        // Test TitleSource values
        val yamlFilename = """
            confluence:
              base_url: "https://test.com"
              pat: "token"
            space:
              key: "TEST"
            content:
              title_source: filename
        """.trimIndent()
        assertEquals(TitleSource.FILENAME, loader.parse(yamlFilename).content.titleSource)

        val yamlFirstHeading = """
            confluence:
              base_url: "https://test.com"
              pat: "token"
            space:
              key: "TEST"
            content:
              title_source: first_heading
        """.trimIndent()
        assertEquals(TitleSource.FIRST_HEADING, loader.parse(yamlFirstHeading).content.titleSource)

        // Test ConflictResolution values
        val yamlRemote = """
            confluence:
              base_url: "https://test.com"
              pat: "token"
            space:
              key: "TEST"
            sync:
              conflict_resolution: remote
        """.trimIndent()
        assertEquals(ConflictResolution.REMOTE, loader.parse(yamlRemote).sync.conflictResolution)
    }
}
