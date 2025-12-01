package com.consync.config

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigValidatorTest {

    private val validator = ConfigValidator()

    private fun validConfig() = Config(
        confluence = ConfluenceConfig(
            baseUrl = "https://example.atlassian.net/wiki",
            username = "user@example.com",
            apiToken = "token123"
        ),
        space = SpaceConfig(
            key = "DOCS"
        )
    )

    @Test
    fun `should pass validation for valid config`() {
        val config = validConfig()
        val errors = validator.validate(config)
        assertTrue(errors.isEmpty(), "Expected no errors but got: $errors")
    }

    @Test
    fun `should fail for blank base url`() {
        val config = validConfig().copy(
            confluence = validConfig().confluence.copy(baseUrl = "")
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("base_url") && it.contains("required") })
    }

    @Test
    fun `should fail for invalid base url`() {
        val config = validConfig().copy(
            confluence = validConfig().confluence.copy(baseUrl = "not-a-url")
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("base_url") && it.contains("valid URL") })
    }

    @Test
    fun `should fail for missing authentication`() {
        val config = validConfig().copy(
            confluence = ConfluenceConfig(
                baseUrl = "https://example.atlassian.net/wiki"
            )
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("Authentication required") })
    }

    @Test
    fun `should fail for both auth methods provided`() {
        val config = validConfig().copy(
            confluence = ConfluenceConfig(
                baseUrl = "https://example.atlassian.net/wiki",
                username = "user@example.com",
                apiToken = "token",
                pat = "pat-token"
            )
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("only one authentication method") })
    }

    @Test
    fun `should pass with PAT authentication`() {
        val config = validConfig().copy(
            confluence = ConfluenceConfig(
                baseUrl = "https://example.atlassian.net/wiki",
                pat = "pat-token"
            )
        )
        val errors = validator.validate(config)
        assertTrue(errors.isEmpty(), "Expected no errors but got: $errors")
    }

    @Test
    fun `should fail for blank space key`() {
        val config = validConfig().copy(
            space = SpaceConfig(key = "")
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("space.key") && it.contains("required") })
    }

    @Test
    fun `should fail for invalid space key format`() {
        val invalidKeys = listOf("docs", "My Space", "123ABC", "my-space", "DOCS!")

        for (key in invalidKeys) {
            val config = validConfig().copy(
                space = SpaceConfig(key = key)
            )
            val errors = validator.validate(config)
            assertTrue(
                errors.any { it.contains("space.key") },
                "Expected error for invalid key '$key' but got: $errors"
            )
        }
    }

    @Test
    fun `should pass for valid space keys`() {
        val validKeys = listOf("DOCS", "MY_SPACE", "PROJECT1", "A", "ABC123")

        for (key in validKeys) {
            val config = validConfig().copy(
                space = SpaceConfig(key = key)
            )
            val errors = validator.validate(config)
            assertTrue(
                errors.none { it.contains("space.key") },
                "Expected no space.key error for '$key' but got: $errors"
            )
        }
    }

    @Test
    fun `should fail for invalid toc depth`() {
        val config = validConfig().copy(
            content = ContentConfig(
                toc = TocConfig(depth = 0)
            )
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("toc.depth") })

        val config2 = validConfig().copy(
            content = ContentConfig(
                toc = TocConfig(depth = 7)
            )
        )
        val errors2 = validator.validate(config2)
        assertTrue(errors2.any { it.contains("toc.depth") })
    }

    @Test
    fun `should fail for empty include patterns`() {
        val config = validConfig().copy(
            files = FilesConfig(include = emptyList())
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("files.include") })
    }

    @Test
    fun `should fail for blank patterns`() {
        val config = validConfig().copy(
            files = FilesConfig(include = listOf("**/*.md", ""))
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("files.include") && it.contains("blank") })
    }

    @Test
    fun `should fail for excessive timeout`() {
        val config = validConfig().copy(
            confluence = validConfig().confluence.copy(timeout = 500)
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("timeout") })
    }

    @Test
    fun `should fail for negative retry count`() {
        val config = validConfig().copy(
            confluence = validConfig().confluence.copy(retryCount = -1)
        )
        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("retry_count") })
    }

    @Test
    fun `should accumulate multiple errors`() {
        val config = Config(
            confluence = ConfluenceConfig(
                baseUrl = "",
                timeout = 0
            ),
            space = SpaceConfig(key = ""),
            files = FilesConfig(include = emptyList())
        )
        val errors = validator.validate(config)
        assertTrue(errors.size >= 3, "Expected multiple errors but got ${errors.size}: $errors")
    }
}
