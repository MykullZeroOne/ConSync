package com.consync.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Loads and parses ConSync configuration from YAML files.
 */
class ConfigLoader {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false
        )
    )

    /**
     * Load configuration from the specified path.
     *
     * @param path Path to the consync.yaml file
     * @return Parsed configuration
     * @throws ConfigLoadException if loading or parsing fails
     */
    fun load(path: Path): Config {
        logger.debug("Loading configuration from: $path")

        val content = try {
            path.readText()
        } catch (e: Exception) {
            throw ConfigLoadException("Failed to read configuration file: $path", e)
        }

        return parse(content, path.toString())
    }

    /**
     * Parse configuration from YAML content string.
     *
     * @param content YAML content
     * @param source Source identifier for error messages
     * @return Parsed configuration
     * @throws ConfigLoadException if parsing fails
     */
    fun parse(content: String, source: String = "<string>"): Config {
        logger.debug("Parsing configuration from: $source")

        // Expand environment variables in the content
        val expandedContent = expandEnvironmentVariables(content)

        return try {
            yaml.decodeFromString(Config.serializer(), expandedContent)
        } catch (e: Exception) {
            throw ConfigLoadException("Failed to parse configuration from $source: ${e.message}", e)
        }
    }

    /**
     * Expand environment variable references in the configuration.
     * Supports ${VAR_NAME} syntax.
     */
    private fun expandEnvironmentVariables(content: String): String {
        val envVarPattern = """\$\{([A-Za-z_][A-Za-z0-9_]*)}""".toRegex()

        return envVarPattern.replace(content) { matchResult ->
            val varName = matchResult.groupValues[1]
            val value = System.getenv(varName)

            if (value != null) {
                logger.debug("Expanded environment variable: $varName")
                value
            } else {
                logger.warn("Environment variable not found: $varName")
                matchResult.value // Keep original if not found
            }
        }
    }
}

/**
 * Exception thrown when configuration loading fails.
 */
class ConfigLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
