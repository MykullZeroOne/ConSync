package com.consync.cli

import com.consync.client.confluence.ConfluenceClientFactory
import com.consync.config.ConfigLoader
import com.consync.config.ConfigValidator
import com.consync.service.SyncService
import com.consync.service.SyncStateManager
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
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
        echo("")

        // Create Confluence client
        val client = try {
            ConfluenceClientFactory.create(config.confluence)
        } catch (e: Exception) {
            echo("Error creating Confluence client: ${e.message}", err = true)
            logger.error("Failed to create Confluence client", e)
            throw e
        }

        // Create state manager
        val stateDir = directory.resolve(".consync")
        val stateManager = SyncStateManager(stateDir)

        // Create sync service
        val syncService = SyncService(
            config = config,
            contentDir = directory,
            client = client,
            stateManager = stateManager
        )

        // Perform sync
        try {
            val result = runBlocking {
                syncService.sync(dryRun = dryRun, force = force)
            }

            // Display results
            echo("")
            echo("Sync completed ${if (result.success) "successfully" else "with errors"}")
            echo("  Duration: ${result.duration.toMillis()}ms")
            echo("")
            echo("Results:")
            echo("  Created: ${result.createdCount}")
            echo("  Updated: ${result.updatedCount}")
            echo("  Deleted: ${result.deletedCount}")
            echo("  Moved: ${result.movedCount}")
            echo("  Skipped: ${result.skippedCount}")
            if (result.failedCount > 0) {
                echo("  Failed: ${result.failedCount}")
            }

            if (result.failedActions.isNotEmpty()) {
                echo("")
                echo("Failures:")
                result.failedActions.forEach { actionResult ->
                    echo("  - ${actionResult.title}: ${actionResult.errorMessage ?: "Unknown error"}")
                }
            }

            if (!result.success) {
                throw RuntimeException("Sync completed with errors")
            }
        } catch (e: Exception) {
            echo("", err = true)
            echo("Sync failed: ${e.message}", err = true)
            logger.error("Sync operation failed", e)
            throw e
        }
    }
}
