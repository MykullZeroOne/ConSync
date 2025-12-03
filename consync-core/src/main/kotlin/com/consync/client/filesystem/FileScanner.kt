package com.consync.client.filesystem

import com.consync.config.FilesConfig
import org.slf4j.LoggerFactory
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*

/**
 * Scans directories for markdown files based on include/exclude patterns.
 */
class FileScanner(
    private val config: FilesConfig = FilesConfig()
) {
    private val logger = LoggerFactory.getLogger(FileScanner::class.java)

    /**
     * Scan a directory for markdown files matching the configured patterns.
     *
     * @param rootDir The root directory to scan
     * @return List of paths to matching files, relative to rootDir
     */
    fun scan(rootDir: Path): List<Path> {
        logger.info("Scanning directory: $rootDir")
        logger.debug("Include patterns: ${config.include}")
        logger.debug("Exclude patterns: ${config.exclude}")

        if (!rootDir.exists()) {
            throw IllegalArgumentException("Directory does not exist: $rootDir")
        }

        if (!rootDir.isDirectory()) {
            throw IllegalArgumentException("Path is not a directory: $rootDir")
        }

        val matchedFiles = mutableListOf<Path>()
        val includeMatchers = config.include.map { createGlobMatcher(it) }
        val excludeMatchers = config.exclude.map { createGlobMatcher(it) }

        Files.walkFileTree(rootDir, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = rootDir.relativize(file)

                // Check if file matches any include pattern
                val included = includeMatchers.any { it.matches(relativePath) }
                if (!included) {
                    return FileVisitResult.CONTINUE
                }

                // Check if file matches any exclude pattern
                val excluded = excludeMatchers.any { it.matches(relativePath) }
                if (excluded) {
                    logger.debug("Excluded: $relativePath")
                    return FileVisitResult.CONTINUE
                }

                logger.debug("Matched: $relativePath")
                matchedFiles.add(relativePath)
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = rootDir.relativize(dir)

                // Skip hidden directories
                if (dir.fileName?.toString()?.startsWith(".") == true && dir != rootDir) {
                    logger.debug("Skipping hidden directory: $relativePath")
                    return FileVisitResult.SKIP_SUBTREE
                }

                // Check if directory matches any exclude pattern
                val dirExcluded = excludeMatchers.any { matcher ->
                    // Check both with and without trailing slash
                    matcher.matches(relativePath) ||
                    matcher.matches(relativePath.resolve(""))
                }

                if (dirExcluded) {
                    logger.debug("Skipping excluded directory: $relativePath")
                    return FileVisitResult.SKIP_SUBTREE
                }

                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: java.io.IOException): FileVisitResult {
                logger.warn("Failed to access file: $file", exc)
                return FileVisitResult.CONTINUE
            }
        })

        logger.info("Found ${matchedFiles.size} matching files")
        return matchedFiles.sortedBy { it.toString() }
    }

    /**
     * Read file content.
     *
     * @param rootDir The root directory
     * @param relativePath Relative path to the file
     * @return File content as string
     */
    fun readFile(rootDir: Path, relativePath: Path): String {
        val fullPath = rootDir.resolve(relativePath)
        return fullPath.readText()
    }

    /**
     * Get file last modified time.
     *
     * @param rootDir The root directory
     * @param relativePath Relative path to the file
     * @return Last modified instant
     */
    fun getLastModified(rootDir: Path, relativePath: Path): java.time.Instant {
        val fullPath = rootDir.resolve(relativePath)
        return Files.getLastModifiedTime(fullPath).toInstant()
    }

    /**
     * Check if a file exists.
     *
     * @param rootDir The root directory
     * @param relativePath Relative path to check
     * @return true if file exists
     */
    fun exists(rootDir: Path, relativePath: Path): Boolean {
        return rootDir.resolve(relativePath).exists()
    }

    /**
     * Create a glob pattern matcher.
     */
    private fun createGlobMatcher(pattern: String): PathMatcher {
        // Normalize pattern for glob syntax
        val normalizedPattern = pattern
            .replace("\\", "/")  // Normalize path separators

        return FileSystems.getDefault().getPathMatcher("glob:$normalizedPattern")
    }

    companion object {
        /**
         * Default file configuration for markdown files.
         */
        val DEFAULT_CONFIG = FilesConfig(
            include = listOf("**/*.md"),
            exclude = listOf(
                "**/node_modules/**",
                "**/.git/**",
                "**/vendor/**",
                "**/_*/**"  // Exclude directories starting with underscore
            )
        )
    }
}
