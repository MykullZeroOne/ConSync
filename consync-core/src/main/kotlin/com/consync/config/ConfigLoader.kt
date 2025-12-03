package com.consync.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.consync.util.EnvLoader
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Loads and parses ConSync configuration from YAML files.
 *
 * Supports environment variable expansion using ${VAR_NAME} syntax.
 * Variables are resolved from:
 * 1. .env file (if present and loadEnvFile is true)
 * 2. System environment variables
 */
class ConfigLoader(
    private val loadEnvFile: Boolean = true,
    private val envFilePath: Path? = null
) {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false
        )
    )

    init {
        if (loadEnvFile) {
            if (envFilePath != null) {
                EnvLoader.load(envFilePath)
            } else {
                EnvLoader.loadDefault()
            }
        }
    }

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
     * Looks up variables from EnvLoader (which checks .env first, then system env).
     */
    private fun expandEnvironmentVariables(content: String): String {
        val envVarPattern = """\$\{([A-Za-z_][A-Za-z0-9_]*)}""".toRegex()

        return envVarPattern.replace(content) { matchResult ->
            val varName = matchResult.groupValues[1]
            // Use EnvLoader which checks .env vars first, then system env
            val value = EnvLoader.get(varName)

            if (value != null) {
                logger.debug("Expanded environment variable: $varName")
                value
            } else {
                logger.debug("Environment variable not found: $varName (keeping original)")
                matchResult.value // Keep original if not found
            }
        }
    }

    companion object {
        /**
         * Create a ConfigLoader that loads .env from the specified directory.
         */
        fun withEnvFile(envFilePath: Path): ConfigLoader {
            return ConfigLoader(loadEnvFile = true, envFilePath = envFilePath)
        }

        /**
         * Create a ConfigLoader that doesn't load any .env file.
         */
        fun withoutEnvFile(): ConfigLoader {
            return ConfigLoader(loadEnvFile = false)
        }
    }
}

/**
 * Exception thrown when configuration loading fails.
 */
class ConfigLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
