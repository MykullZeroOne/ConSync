package com.consync.util

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

/**
 * Loads environment variables from .env files.
 *
 * Supports standard .env file format:
 * - KEY=value
 * - KEY="quoted value"
 * - KEY='single quoted value'
 * - # comments
 * - Empty lines are ignored
 */
object EnvLoader {
    private val logger = LoggerFactory.getLogger(EnvLoader::class.java)

    /** In-memory storage for loaded env vars (used when we can't set system env) */
    private val loadedVars = mutableMapOf<String, String>()

    /**
     * Load environment variables from a .env file.
     *
     * @param path Path to the .env file
     * @param override If true, override existing environment variables
     * @return Map of loaded variables
     */
    fun load(path: Path, override: Boolean = false): Map<String, String> {
        if (!path.exists()) {
            logger.debug(".env file not found at: {}", path)
            return emptyMap()
        }

        logger.info("Loading environment variables from: {}", path)

        val loaded = mutableMapOf<String, String>()

        try {
            path.readLines().forEachIndexed { lineNum, line ->
                val trimmed = line.trim()

                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEachIndexed
                }

                // Parse KEY=value
                val parsed = parseLine(trimmed, lineNum + 1)
                if (parsed != null) {
                    val (key, value) = parsed

                    // Check if we should set this variable
                    val existingValue = System.getenv(key) ?: loadedVars[key]
                    if (existingValue == null || override) {
                        loadedVars[key] = value
                        loaded[key] = value
                        logger.debug("Loaded env var: {} = {}", key, maskValue(value))
                    } else {
                        logger.debug("Skipping existing env var: {}", key)
                    }
                }
            }

            logger.info("Loaded {} environment variables from .env", loaded.size)
        } catch (e: Exception) {
            logger.error("Failed to load .env file: {}", e.message)
        }

        return loaded
    }

    /**
     * Load from default .env location (current directory).
     */
    fun loadDefault(override: Boolean = false): Map<String, String> {
        return load(Path.of(".env"), override)
    }

    /**
     * Load from multiple potential locations.
     * First file found wins.
     */
    fun loadFromLocations(vararg paths: Path, override: Boolean = false): Map<String, String> {
        for (path in paths) {
            if (path.exists()) {
                return load(path, override)
            }
        }
        logger.debug("No .env file found in any of the specified locations")
        return emptyMap()
    }

    /**
     * Get an environment variable, checking loaded vars first then system env.
     */
    fun get(key: String): String? {
        return loadedVars[key] ?: System.getenv(key)
    }

    /**
     * Get an environment variable with a default value.
     */
    fun get(key: String, default: String): String {
        return get(key) ?: default
    }

    /**
     * Get all loaded variables.
     */
    fun getAll(): Map<String, String> = loadedVars.toMap()

    /**
     * Clear all loaded variables.
     */
    fun clear() {
        loadedVars.clear()
    }

    /**
     * Parse a single line from the .env file.
     */
    private fun parseLine(line: String, lineNum: Int): Pair<String, String>? {
        val equalsIndex = line.indexOf('=')
        if (equalsIndex == -1) {
            logger.warn("Invalid line {} in .env file (no '=' found): {}", lineNum, line)
            return null
        }

        val key = line.substring(0, equalsIndex).trim()
        var value = line.substring(equalsIndex + 1).trim()

        // Validate key
        if (!key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
            logger.warn("Invalid environment variable name on line {}: {}", lineNum, key)
            return null
        }

        // Remove quotes if present
        value = unquote(value)

        // Handle escape sequences
        value = processEscapes(value)

        return key to value
    }

    /**
     * Remove surrounding quotes from a value.
     */
    private fun unquote(value: String): String {
        if (value.length < 2) return value

        return when {
            value.startsWith("\"") && value.endsWith("\"") -> value.substring(1, value.length - 1)
            value.startsWith("'") && value.endsWith("'") -> value.substring(1, value.length - 1)
            else -> value
        }
    }

    /**
     * Process escape sequences in a value.
     */
    private fun processEscapes(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\\\", "\\")
    }

    /**
     * Mask sensitive values for logging.
     */
    private fun maskValue(value: String): String {
        return if (value.length > 4) {
            "${value.take(2)}${"*".repeat(minOf(value.length - 4, 10))}${value.takeLast(2)}"
        } else {
            "****"
        }
    }
}

/**
 * Extension function to expand environment variables in a string.
 * Uses EnvLoader for variable lookup.
 */
fun String.expandEnvVars(): String {
    val envVarPattern = """\$\{([A-Za-z_][A-Za-z0-9_]*)}""".toRegex()

    return envVarPattern.replace(this) { matchResult ->
        val varName = matchResult.groupValues[1]
        EnvLoader.get(varName) ?: matchResult.value
    }
}
