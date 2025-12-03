package com.consync.client.confluence

import com.consync.client.confluence.model.*
import java.io.File

/**
 * Interface for Confluence REST API operations.
 *
 * All methods are suspending functions to support async operations.
 * Implementations should handle authentication, retries, and error mapping.
 */
interface ConfluenceClient {

    // ========== Space Operations ==========

    /**
     * Get space information by key.
     *
     * @param spaceKey The space key (e.g., "DOCS")
     * @return Space information
     * @throws NotFoundException if space doesn't exist
     */
    suspend fun getSpace(spaceKey: String): Space

    // ========== Page Operations ==========

    /**
     * Get a page by ID.
     *
     * @param pageId The page ID
     * @param expand Optional list of fields to expand (e.g., "body.storage", "version", "ancestors")
     * @return Page information
     * @throws NotFoundException if page doesn't exist
     */
    suspend fun getPage(pageId: String, expand: List<String> = emptyList()): Page

    /**
     * Find a page by title within a space.
     *
     * @param spaceKey The space key
     * @param title The page title (exact match)
     * @return Page if found, null otherwise
     */
    suspend fun getPageByTitle(spaceKey: String, title: String): Page?

    /**
     * Create a new page.
     *
     * @param request The create page request
     * @return The created page
     * @throws ConflictException if page with same title exists
     */
    suspend fun createPage(request: CreatePageRequest): Page

    /**
     * Update an existing page.
     *
     * @param pageId The page ID to update
     * @param request The update page request
     * @return The updated page
     * @throws NotFoundException if page doesn't exist
     * @throws ConflictException if version conflict
     */
    suspend fun updatePage(pageId: String, request: UpdatePageRequest): Page

    /**
     * Delete a page.
     *
     * @param pageId The page ID to delete
     * @throws NotFoundException if page doesn't exist
     */
    suspend fun deletePage(pageId: String)

    // ========== Hierarchy Operations ==========

    /**
     * Get child pages of a parent page.
     *
     * @param pageId The parent page ID
     * @param expand Optional list of fields to expand
     * @param limit Maximum number of results (default 25)
     * @return List of child pages
     */
    suspend fun getChildPages(
        pageId: String,
        expand: List<String> = emptyList(),
        limit: Int = 25
    ): List<Page>

    /**
     * Get all child pages recursively.
     *
     * @param pageId The parent page ID
     * @param expand Optional list of fields to expand
     * @return List of all descendant pages
     */
    suspend fun getDescendantPages(
        pageId: String,
        expand: List<String> = emptyList()
    ): List<Page>

    /**
     * Get ancestors of a page.
     *
     * @param pageId The page ID
     * @return List of ancestor pages (from root to immediate parent)
     */
    suspend fun getAncestors(pageId: String): List<Page>

    /**
     * Move a page to a new parent.
     *
     * @param pageId The page ID to move
     * @param newParentId The new parent page ID
     * @return The updated page
     */
    suspend fun movePage(pageId: String, newParentId: String): Page

    // ========== Search Operations ==========

    /**
     * Search for content using CQL (Confluence Query Language).
     *
     * @param cql The CQL query string
     * @param limit Maximum number of results (default 25)
     * @param start Starting index for pagination
     * @param expand Optional list of fields to expand
     * @return Search results
     */
    suspend fun search(
        cql: String,
        limit: Int = 25,
        start: Int = 0,
        expand: List<String> = emptyList()
    ): SearchResult

    /**
     * Get all pages in a space.
     *
     * @param spaceKey The space key
     * @param expand Optional list of fields to expand
     * @param limit Maximum number of results per request
     * @return List of all pages in the space
     */
    suspend fun getAllPagesInSpace(
        spaceKey: String,
        expand: List<String> = emptyList(),
        limit: Int = 100
    ): List<Page>

    // ========== Attachment Operations ==========

    /**
     * Get attachments for a page.
     *
     * @param pageId The page ID
     * @param limit Maximum number of results
     * @return List of attachments
     */
    suspend fun getAttachments(pageId: String, limit: Int = 100): List<Attachment>

    /**
     * Upload an attachment to a page.
     *
     * @param pageId The page ID
     * @param file The file to upload
     * @param comment Optional comment for the attachment
     * @return The created attachment
     */
    suspend fun uploadAttachment(
        pageId: String,
        file: File,
        comment: String? = null
    ): Attachment

    /**
     * Update an existing attachment.
     *
     * @param pageId The page ID
     * @param attachmentId The attachment ID
     * @param file The new file content
     * @param comment Optional comment
     * @return The updated attachment
     */
    suspend fun updateAttachment(
        pageId: String,
        attachmentId: String,
        file: File,
        comment: String? = null
    ): Attachment

    // ========== Utility Operations ==========

    /**
     * Test the connection and authentication.
     *
     * @return true if connection is successful
     * @throws AuthenticationException if authentication fails
     */
    suspend fun testConnection(): Boolean

    /**
     * Close the client and release resources.
     */
    fun close()
}
