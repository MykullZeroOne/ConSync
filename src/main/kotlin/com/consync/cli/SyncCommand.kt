package com.consync.cli

import com.consync.config.ConfigLoader
import com.consync.config.ConfigValidator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class SyncCommand : CliktCommand(
    name = "sync",
    help = "Synchronize local Markdown files to Confluence"
) {
    private val logger = LoggerFactory.getLogger(SyncCommand::class.java)

    private val directory by argument(
        name = "directory",
        help = "Directory containing Markdown files and consync.yaml"
    ).path(mustExist = true, canBeFile = false).default(Path.of("."))

    private val dryRun by option(
        "--dry-run", "-n",
        help = "Preview changes without pushing to Confluence"
    ).flag(default = false)

    private val force by option(
        "--force", "-f",
        help = "Force update all pages regardless of change detection"
    ).flag(default = false)

    private val configFile by option(
        "--config", "-c",
        help = "Specify alternate config file location"
    ).path()

    private val spaceKey by option(
        "--space", "-s",
        help = "Override space key from config"
    )

    override fun help(context: Context): String = """
        Synchronize local Markdown files to Confluence.

        This command reads Markdown files from the specified directory,
        converts them to Confluence storage format, and creates or updates
        pages in your Confluence space.

        Examples:
          consync sync .
          consync sync /path/to/docs
          consync sync --dry-run /path/to/docs
          consync sync --force --space MYSPACE /path/to/docs
    """.trimIndent()

    override fun run() {
        logger.info("Starting sync from directory: $directory")

        // Resolve config file path
        val configPath = configFile ?: directory.resolve("consync.yaml")

        if (!configPath.exists()) {
            echo("Error: Configuration file not found: $configPath", err = true)
            echo("Create a consync.yaml file in your documentation directory.", err = true)
            throw RuntimeException("Configuration file not found")
        }

        // Load and validate configuration
        val config = try {
            val loader = ConfigLoader()
            val loadedConfig = loader.load(configPath)

            // Apply CLI overrides
            if (spaceKey != null) {
                loadedConfig.copy(
                    space = loadedConfig.space.copy(key = spaceKey!!)
                )
            } else {
                loadedConfig
            }
        } catch (e: Exception) {
            echo("Error loading configuration: ${e.message}", err = true)
            logger.error("Failed to load configuration", e)
            throw e
        }

        // Validate configuration
        val validator = ConfigValidator()
        val validationErrors = validator.validate(config)
        if (validationErrors.isNotEmpty()) {
            echo("Configuration validation errors:", err = true)
            validationErrors.forEach { echo("  - $it", err = true) }
            throw RuntimeException("Configuration validation failed")
        }

        if (dryRun) {
            echo("DRY RUN MODE - No changes will be made to Confluence")
        }

        echo("Configuration loaded successfully")
        echo("  Space: ${config.space.key}")
        echo("  Base URL: ${config.confluence.baseUrl}")

        // TODO: Implement sync logic in Phase 6
        echo("")
        echo("Sync functionality will be implemented in Phase 6.")
        echo("Current phase: Project setup and configuration loading.")
    }
}
