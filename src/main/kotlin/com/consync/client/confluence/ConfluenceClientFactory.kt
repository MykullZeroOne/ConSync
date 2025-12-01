package com.consync.client.confluence

import com.consync.config.Config
import com.consync.config.ConfluenceConfig
import org.slf4j.LoggerFactory

/**
 * Factory for creating ConfluenceClient instances.
 */
object ConfluenceClientFactory {

    private val logger = LoggerFactory.getLogger(ConfluenceClientFactory::class.java)

    /**
     * Create a ConfluenceClient from application configuration.
     *
     * @param config The full application configuration
     * @return A configured ConfluenceClient instance
     */
    fun create(config: Config): ConfluenceClient {
        return create(config.confluence)
    }

    /**
     * Create a ConfluenceClient from Confluence-specific configuration.
     *
     * @param config The Confluence configuration
     * @return A configured ConfluenceClient instance
     */
    fun create(config: ConfluenceConfig): ConfluenceClient {
        logger.info("Creating Confluence client for: ${config.baseUrl}")

        val authMethod = when {
            !config.pat.isNullOrBlank() -> "PAT"
            !config.username.isNullOrBlank() && !config.apiToken.isNullOrBlank() -> "API Token"
            else -> "None"
        }
        logger.debug("Authentication method: $authMethod")

        return ConfluenceClientImpl(config)
    }

    /**
     * Create a ConfluenceClient for Confluence Cloud.
     *
     * @param baseUrl The Confluence Cloud URL (e.g., "https://your-domain.atlassian.net/wiki")
     * @param username The user's email address
     * @param apiToken The API token
     * @param timeout Request timeout in seconds
     * @param retryCount Number of retry attempts
     * @return A configured ConfluenceClient instance
     */
    fun createCloudClient(
        baseUrl: String,
        username: String,
        apiToken: String,
        timeout: Int = 30,
        retryCount: Int = 3
    ): ConfluenceClient {
        val config = ConfluenceConfig(
            baseUrl = baseUrl,
            username = username,
            apiToken = apiToken,
            timeout = timeout,
            retryCount = retryCount
        )
        return create(config)
    }

    /**
     * Create a ConfluenceClient for Confluence Data Center/Server.
     *
     * @param baseUrl The Confluence server URL
     * @param pat Personal Access Token
     * @param timeout Request timeout in seconds
     * @param retryCount Number of retry attempts
     * @return A configured ConfluenceClient instance
     */
    fun createServerClient(
        baseUrl: String,
        pat: String,
        timeout: Int = 30,
        retryCount: Int = 3
    ): ConfluenceClient {
        val config = ConfluenceConfig(
            baseUrl = baseUrl,
            pat = pat,
            timeout = timeout,
            retryCount = retryCount
        )
        return create(config)
    }
}
