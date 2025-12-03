package com.consync.core.markdown

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.slf4j.LoggerFactory

/**
 * Parses YAML frontmatter from markdown documents.
 *
 * Frontmatter is enclosed between --- delimiters at the start of the file:
 * ```
 * ---
 * title: My Page
 * tags: [doc, guide]
 * ---
 * # Content starts here
 * ```
 */
class FrontmatterParser {
    private val logger = LoggerFactory.getLogger(FrontmatterParser::class.java)

    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false
        )
    )

    /**
     * Parse frontmatter from markdown content.
     *
     * @param content Raw markdown content
     * @return Pair of (Frontmatter, content without frontmatter)
     */
    fun parse(content: String): Pair<Frontmatter, String> {
        val trimmedContent = content.trimStart()

        // Check if content starts with frontmatter delimiter
        if (!trimmedContent.startsWith("---")) {
            return Frontmatter.EMPTY to content
        }

        // Find the closing delimiter
        val endIndex = trimmedContent.indexOf("---", 3)
        if (endIndex == -1) {
            logger.warn("Frontmatter opening delimiter found but no closing delimiter")
            return Frontmatter.EMPTY to content
        }

        // Extract frontmatter YAML
        val frontmatterYaml = trimmedContent.substring(3, endIndex).trim()
        val remainingContent = trimmedContent.substring(endIndex + 3).trimStart()

        if (frontmatterYaml.isEmpty()) {
            return Frontmatter.EMPTY to remainingContent
        }

        // Parse YAML
        val frontmatter = try {
            parseFrontmatterYaml(frontmatterYaml)
        } catch (e: Exception) {
            logger.warn("Failed to parse frontmatter YAML: ${e.message}")
            Frontmatter.EMPTY
        }

        return frontmatter to remainingContent
    }

    /**
     * Check if content has frontmatter.
     */
    fun hasFrontmatter(content: String): Boolean {
        val trimmed = content.trimStart()
        if (!trimmed.startsWith("---")) return false
        return trimmed.indexOf("---", 3) != -1
    }

    /**
     * Extract just the frontmatter YAML string.
     */
    fun extractFrontmatterYaml(content: String): String? {
        val trimmed = content.trimStart()
        if (!trimmed.startsWith("---")) return null

        val endIndex = trimmed.indexOf("---", 3)
        if (endIndex == -1) return null

        return trimmed.substring(3, endIndex).trim()
    }

    /**
     * Strip frontmatter from content, returning only the markdown body.
     */
    fun stripFrontmatter(content: String): String {
        return parse(content).second
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFrontmatterYaml(yamlContent: String): Frontmatter {
        // Parse as generic map first for flexibility
        val map = try {
            yaml.decodeFromString(
                kotlinx.serialization.serializer<Map<String, Any?>>(),
                yamlContent
            )
        } catch (e: Exception) {
            logger.debug("Failed to parse as Map, trying alternative parsing: ${e.message}")
            return Frontmatter.EMPTY
        }

        return Frontmatter(
            title = map["title"]?.toString(),
            description = map["description"]?.toString(),
            tags = extractTags(map["tags"]),
            author = map["author"]?.toString(),
            date = map["date"]?.toString(),
            weight = extractInt(map["weight"]),
            nav = map["nav"]?.toString()?.toBoolean() ?: true,
            confluenceId = map["confluence_id"]?.toString() ?: map["confluenceId"]?.toString(),
            parent = map["parent"]?.toString(),
            custom = extractCustomProperties(map)
        )
    }

    private fun extractTags(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is String -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
    }

    private fun extractInt(value: Any?): Int? {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun extractCustomProperties(map: Map<String, Any?>): Map<String, Any> {
        val knownKeys = setOf(
            "title", "description", "tags", "author", "date",
            "weight", "nav", "confluence_id", "confluenceId", "parent"
        )
        return map.filterKeys { it !in knownKeys }
            .mapValues { it.value ?: "" }
            .filterValues { it != "" }
            .mapValues { it.value as Any }
    }
}
