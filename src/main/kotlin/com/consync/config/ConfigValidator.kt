package com.consync.config

import org.slf4j.LoggerFactory

/**
 * Validates ConSync configuration for correctness and completeness.
 */
class ConfigValidator {
    private val logger = LoggerFactory.getLogger(ConfigValidator::class.java)

    /**
     * Validate the configuration and return a list of errors.
     *
     * @param config Configuration to validate
     * @return List of validation error messages (empty if valid)
     */
    fun validate(config: Config): List<String> {
        val errors = mutableListOf<String>()

        validateConfluence(config.confluence, errors)
        validateSpace(config.space, errors)
        validateContent(config.content, errors)
        validateFiles(config.files, errors)

        if (errors.isNotEmpty()) {
            logger.warn("Configuration validation found ${errors.size} error(s)")
        } else {
            logger.debug("Configuration validation passed")
        }

        return errors
    }

    private fun validateConfluence(config: ConfluenceConfig, errors: MutableList<String>) {
        // Validate base URL
        if (config.baseUrl.isBlank()) {
            errors.add("confluence.base_url is required")
        } else if (!isValidUrl(config.baseUrl)) {
            errors.add("confluence.base_url must be a valid URL")
        }

        // Validate authentication
        val hasApiTokenAuth = !config.username.isNullOrBlank() && !config.apiToken.isNullOrBlank()
        val hasPatAuth = !config.pat.isNullOrBlank()

        if (!hasApiTokenAuth && !hasPatAuth) {
            errors.add("Authentication required: provide either (username + api_token) or (pat)")
        }

        if (hasApiTokenAuth && hasPatAuth) {
            errors.add("Provide only one authentication method: (username + api_token) OR (pat)")
        }

        // Validate timeout
        if (config.timeout < 1) {
            errors.add("confluence.timeout must be at least 1 second")
        }

        if (config.timeout > 300) {
            errors.add("confluence.timeout should not exceed 300 seconds")
        }

        // Validate retry count
        if (config.retryCount < 0) {
            errors.add("confluence.retry_count cannot be negative")
        }

        if (config.retryCount > 10) {
            errors.add("confluence.retry_count should not exceed 10")
        }
    }

    private fun validateSpace(config: SpaceConfig, errors: MutableList<String>) {
        if (config.key.isBlank()) {
            errors.add("space.key is required")
        } else if (!isValidSpaceKey(config.key)) {
            errors.add("space.key must contain only uppercase letters, numbers, and underscores")
        }
    }

    private fun validateContent(config: ContentConfig, errors: MutableList<String>) {
        // Validate TOC depth
        if (config.toc.depth < 1 || config.toc.depth > 6) {
            errors.add("content.toc.depth must be between 1 and 6")
        }
    }

    private fun validateFiles(config: FilesConfig, errors: MutableList<String>) {
        if (config.include.isEmpty()) {
            errors.add("files.include must have at least one pattern")
        }

        // Validate patterns are not empty
        config.include.forEachIndexed { index, pattern ->
            if (pattern.isBlank()) {
                errors.add("files.include[$index] cannot be blank")
            }
        }

        config.exclude.forEachIndexed { index, pattern ->
            if (pattern.isBlank()) {
                errors.add("files.exclude[$index] cannot be blank")
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val parsed = java.net.URI(url)
            parsed.scheme in listOf("http", "https") && !parsed.host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidSpaceKey(key: String): Boolean {
        // Confluence space keys: uppercase letters, numbers, underscores
        // Must start with a letter
        return key.matches(Regex("^[A-Z][A-Z0-9_]*$"))
    }
}
