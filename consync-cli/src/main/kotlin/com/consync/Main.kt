package com.consync

import com.consync.cli.ConSyncCommand
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.consync.Main")

fun main(args: Array<String>) {
    logger.debug("Starting ConSync with args: ${args.joinToString(" ")}")
    ConSyncCommand().main(args)
}
