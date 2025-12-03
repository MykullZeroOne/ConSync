package com.consync.cli

import com.consync.config.ConfigLoader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.path
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.exists

class StatusCommand : CliktCommand(
    name = "status",
    help = "Show current sync status"
) {
    private val logger = LoggerFactory.getLogger(StatusCommand::class.java)

    private val directory by argument(
        name = "directory",
        help = "Directory containing Markdown files"
    ).path(mustExist = true, canBeFile = false).default(Path.of("."))

    override fun run() {
        val configPath = directory.resolve("consync.yaml")

        if (!configPath.exists()) {
            echo("Error: Configuration file not found: $configPath", err = true)
            throw RuntimeException("Configuration file not found")
        }

        val statePath = directory.resolve(".consync").resolve("state.json")

        echo("ConSync Status")
        echo("==============")
        echo("")
        echo("Directory: $directory")
        echo("Config: $configPath")
        echo("")

        if (statePath.exists()) {
            echo("Sync State: Found at $statePath")
            // TODO: Parse and display state information in Phase 6
            echo("")
            echo("State details will be available after implementing sync in Phase 6.")
        } else {
            echo("Sync State: Not found")
            echo("")
            echo("No previous sync has been performed.")
            echo("Run 'consync sync $directory' to perform initial sync.")
        }

        // TODO: Show file statistics, last sync time, pending changes
        echo("")
        echo("Full status functionality will be implemented in Phase 6.")
    }
}
