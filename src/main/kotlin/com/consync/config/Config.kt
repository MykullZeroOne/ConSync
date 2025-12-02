package com.consync.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root configuration for ConSync.
 * Loaded from consync.yaml in the documentation directory.
 */
@Serializable
data class Config(
    val confluence: ConfluenceConfig,
    val space: SpaceConfig,
    val content: ContentConfig = ContentConfig(),
    val sync: SyncConfig = SyncConfig(),
    val files: FilesConfig = FilesConfig(),
    val logging: LoggingConfig = LoggingConfig()
)

/**
 * Confluence connection settings.
 */
@Serializable
data class ConfluenceConfig(
    @SerialName("base_url")
    val baseUrl: String,

    val username: String? = null,

    @SerialName("api_token")
    val apiToken: String? = null,

    val pat: String? = null,

    val timeout: Int = 30,

    @SerialName("retry_count")
    val retryCount: Int = 3
)

/**
 * Confluence space configuration.
 */
@Serializable
data class SpaceConfig(
    val key: String,

    val name: String? = null,

    @SerialName("root_page_id")
    val rootPageId: String? = null,

    @SerialName("root_page_title")
    val rootPageTitle: String? = null
)

/**
 * Content handling configuration.
 */
@Serializable
data class ContentConfig(
    @SerialName("root_page")
    val rootPage: String? = null,

    @SerialName("title_source")
    val titleSource: TitleSource = TitleSource.FILENAME,

    val toc: TocConfig = TocConfig(),

    val frontmatter: FrontmatterConfig = FrontmatterConfig()
)

/**
 * Source for page titles.
 */
@Serializable
enum class TitleSource {
    @SerialName("filename")
    FILENAME,

    @SerialName("frontmatter")
    FRONTMATTER,

    @SerialName("first_heading")
    FIRST_HEADING
}

/**
 * Table of contents configuration.
 */
@Serializable
data class TocConfig(
    val enabled: Boolean = true,
    val depth: Int = 3,
    val position: TocPosition = TocPosition.TOP
)

/**
 * Position for table of contents.
 */
@Serializable
enum class TocPosition {
    @SerialName("top")
    TOP,

    @SerialName("bottom")
    BOTTOM,

    @SerialName("none")
    NONE
}

/**
 * Frontmatter handling configuration.
 */
@Serializable
data class FrontmatterConfig(
    val strip: Boolean = true,

    @SerialName("use_title")
    val useTitle: Boolean = true
)

/**
 * Sync behavior configuration.
 */
@Serializable
data class SyncConfig(
    @SerialName("delete_orphans")
    val deleteOrphans: Boolean = false,

    @SerialName("update_unchanged")
    val updateUnchanged: Boolean = false,

    @SerialName("preserve_labels")
    val preserveLabels: Boolean = true,

    @SerialName("conflict_resolution")
    val conflictResolution: ConflictResolution = ConflictResolution.LOCAL,

    @SerialName("state_file")
    val stateFile: String = ".consync/state.json"
)

/**
 * Conflict resolution strategy.
 */
@Serializable
enum class ConflictResolution {
    @SerialName("local")
    LOCAL,

    @SerialName("remote")
    REMOTE,

    @SerialName("ask")
    ASK
}

/**
 * File inclusion/exclusion configuration.
 */
@Serializable
data class FilesConfig(
    val include: List<String> = listOf("*.md", "**/*.md"),
    val exclude: List<String> = emptyList(),

    @SerialName("index_file")
    val indexFile: String = "index.md"
)

/**
 * Logging configuration.
 */
@Serializable
data class LoggingConfig(
    val level: LogLevel = LogLevel.INFO,
    val file: String? = null
)

/**
 * Log levels.
 */
@Serializable
enum class LogLevel {
    @SerialName("debug")
    DEBUG,

    @SerialName("info")
    INFO,

    @SerialName("warn")
    WARN,

    @SerialName("error")
    ERROR
}
