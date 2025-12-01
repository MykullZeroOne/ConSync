package com.consync.cli

import com.consync.config.ConfigLoader
import com.consync.config.ConfigValidator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.path
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.exists

class ValidateCommand : CliktCommand(
    name = "validate",
    help = "Validate configuration file"
) {
    private val logger = LoggerFactory.getLogger(ValidateCommand::class.java)

    private val directory by argument(
        name = "directory",
        help = "Directory containing consync.yaml"
    ).path(mustExist = true, canBeFile = false).default(Path.of("."))

    override fun run() {
        val configPath = directory.resolve("consync.yaml")

        if (!configPath.exists()) {
            echo("Error: Configuration file not found: $configPath", err = true)
            throw RuntimeException("Configuration file not found")
        }

        echo("Validating configuration: $configPath")

        try {
            val loader = ConfigLoader()
            val config = loader.load(configPath)

            val validator = ConfigValidator()
            val errors = validator.validate(config)

            if (errors.isEmpty()) {
                echo("")
                echo("Configuration is valid!")
                echo("")
                echo("Summary:")
                echo("  Confluence URL: ${config.confluence.baseUrl}")
                echo("  Space Key: ${config.space.key}")
                echo("  Root Page: ${config.content.rootPage ?: "(space root)"}")
                echo("  TOC Enabled: ${config.content.toc.enabled}")
                echo("  Include Patterns: ${config.files.include.joinToString(", ")}")
                echo("  Exclude Patterns: ${config.files.exclude.joinToString(", ")}")
            } else {
                echo("")
                echo("Configuration has ${errors.size} error(s):", err = true)
                errors.forEach { echo("  - $it", err = true) }
                throw RuntimeException("Configuration validation failed")
            }
        } catch (e: Exception) {
            if (e.message?.contains("validation failed") == true) {
                throw e
            }
            echo("Error parsing configuration: ${e.message}", err = true)
            logger.error("Failed to parse configuration", e)
            throw e
        }
    }
}
