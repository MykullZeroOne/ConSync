package com.consync.maven

import com.consync.client.confluence.ConfluenceClientFactory
import com.consync.config.*
import com.consync.service.SyncService
import com.consync.service.SyncStateManager
import kotlinx.coroutines.runBlocking
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File

/**
 * Maven plugin goal to synchronize Markdown documentation to Confluence.
 *
 * Usage:
 * ```
 * mvn consync:sync
 * ```
 *
 * Configuration example in pom.xml:
 * ```xml
 * <plugin>
 *     <groupId>com.consync</groupId>
 *     <artifactId>consync-maven-plugin</artifactId>
 *     <version>0.1.3</version>
 *     <configuration>
 *         <baseUrl>https://your-domain.atlassian.net/wiki</baseUrl>
 *         <username>your-email@example.com</username>
 *         <apiToken>${env.CONFLUENCE_API_TOKEN}</apiToken>
 *         <spaceKey>DOCS</spaceKey>
 *         <rootPageTitle>My Documentation</rootPageTitle>
 *         <sourceDirectory>${project.basedir}/docs</sourceDirectory>
 *         <dryRun>false</dryRun>
 *     </configuration>
 * </plugin>
 * ```
 */
@Mojo(
    name = "sync",
    defaultPhase = LifecyclePhase.SITE,
    requiresProject = true,
    threadSafe = true
)
class SyncMojo : AbstractMojo() {

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject

    /**
     * Confluence base URL (e.g., https://your-domain.atlassian.net/wiki)
     */
    @Parameter(property = "consync.baseUrl", required = true)
    private lateinit var baseUrl: String

    /**
     * Confluence username/email
     */
    @Parameter(property = "consync.username", required = true)
    private lateinit var username: String

    /**
     * Confluence API token or Personal Access Token
     */
    @Parameter(property = "consync.apiToken", required = true)
    private lateinit var apiToken: String

    /**
     * Confluence space key
     */
    @Parameter(property = "consync.spaceKey", required = true)
    private lateinit var spaceKey: String

    /**
     * Root page title (will be created if it doesn't exist)
     */
    @Parameter(property = "consync.rootPageTitle")
    private var rootPageTitle: String? = null

    /**
     * Root page ID (alternative to rootPageTitle)
     */
    @Parameter(property = "consync.rootPageId")
    private var rootPageId: String? = null

    /**
     * Source directory containing Markdown files
     */
    @Parameter(
        property = "consync.sourceDirectory",
        defaultValue = "\${project.basedir}/docs"
    )
    private lateinit var sourceDirectory: File

    /**
     * Dry run mode - don't actually make changes
     */
    @Parameter(property = "consync.dryRun", defaultValue = "false")
    private var dryRun: Boolean = false

    /**
     * Force update all pages even if unchanged
     */
    @Parameter(property = "consync.force", defaultValue = "false")
    private var force: Boolean = false

    /**
     * Delete orphaned pages not found in local files
     */
    @Parameter(property = "consync.deleteOrphans", defaultValue = "true")
    private var deleteOrphans: Boolean = true

    /**
     * Enable table of contents generation
     */
    @Parameter(property = "consync.tocEnabled", defaultValue = "false")
    private var tocEnabled: Boolean = false

    /**
     * Skip plugin execution
     */
    @Parameter(property = "consync.skip", defaultValue = "false")
    private var skip: Boolean = false

    override fun execute() {
        if (skip) {
            log.info("ConSync execution skipped")
            return
        }

        if (!sourceDirectory.exists()) {
            throw MojoExecutionException("Source directory does not exist: ${sourceDirectory.absolutePath}")
        }

        if (!sourceDirectory.isDirectory) {
            throw MojoExecutionException("Source path is not a directory: ${sourceDirectory.absolutePath}")
        }

        log.info("=".repeat(80))
        log.info("ConSync Maven Plugin - Synchronizing to Confluence")
        log.info("=".repeat(80))
        log.info("Confluence URL: $baseUrl")
        log.info("Space Key: $spaceKey")
        log.info("Source Directory: ${sourceDirectory.absolutePath}")
        log.info("Dry Run: $dryRun")
        log.info("Force: $force")

        try {
            runBlocking {
                sync()
            }
        } catch (e: Exception) {
            log.error("ConSync execution failed", e)
            throw MojoExecutionException("Failed to sync to Confluence: ${e.message}", e)
        }

        log.info("=".repeat(80))
        log.info("ConSync completed successfully")
        log.info("=".repeat(80))
    }

    private suspend fun sync() {
        // Build config
        val config = Config(
            confluence = ConfluenceConfig(
                baseUrl = baseUrl,
                username = username,
                apiToken = apiToken
            ),
            space = SpaceConfig(
                key = spaceKey,
                rootPageId = rootPageId,
                rootPageTitle = rootPageTitle
            ),
            content = ContentConfig(
                toc = TocConfig(enabled = tocEnabled)
            ),
            sync = SyncConfig(
                deleteOrphans = deleteOrphans
            ),
            files = FilesConfig(),
            logging = LoggingConfig()
        )

        // Initialize components
        val client = ConfluenceClientFactory.create(config)
        val stateFilePath = sourceDirectory.toPath().resolve(config.sync.stateFile)
        val stateManager = SyncStateManager(stateFilePath)
        val syncService = SyncService(config, sourceDirectory.toPath(), client, stateManager)

        log.info("Starting sync...")

        try {
            val result = syncService.sync(dryRun = dryRun, force = force)

            log.info("")
            log.info("Sync Summary:")
            log.info("  Created: ${result.createdCount}")
            log.info("  Updated: ${result.updatedCount}")
            log.info("  Deleted: ${result.deletedCount}")
            log.info("  Skipped: ${result.skippedCount}")
            log.info("  Failed:  ${result.failedCount}")

            if (result.failedCount > 0) {
                throw MojoExecutionException("Sync completed with ${result.failedCount} failures")
            }
        } catch (e: Exception) {
            log.error("Sync failed: ${e.message}", e)
            throw e
        }
    }
}
