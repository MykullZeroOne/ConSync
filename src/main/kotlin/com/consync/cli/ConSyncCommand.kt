package com.consync.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

class ConSyncCommand : CliktCommand(
    name = "consync",
    help = "Synchronize local Markdown files with Confluence"
) {
    private val verbose by option("-v", "--verbose", help = "Enable verbose output")
        .flag(default = false)

    private val debug by option("-d", "--debug", help = "Enable debug output")
        .flag(default = false)

    init {
        versionOption("0.1.2")
        subcommands(
            SyncCommand(),
            ValidateCommand(),
            StatusCommand(),
            DiffCommand()
        )
    }

    override fun run() {
        configureLogging()
    }

    private fun configureLogging() {
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        when {
            debug -> rootLogger.level = Level.DEBUG
            verbose -> rootLogger.level = Level.INFO
            else -> rootLogger.level = Level.WARN
        }
    }
}
