package com.consync.client.confluence

import com.consync.client.confluence.exception.*
import com.consync.client.confluence.model.*
import com.consync.config.ConfluenceConfig
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * Implementation of ConfluenceClient using OkHttp.
 *
 * Supports both Confluence Cloud (username + API token) and
 * Confluence Data Center/Server (Personal Access Token).
 */
class ConfluenceClientImpl(
    private val config: ConfluenceConfig
) : ConfluenceClient {

    private val logger = LoggerFactory.getLogger(ConfluenceClientImpl::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.timeout.toLong(), TimeUnit.SECONDS)
        .readTimeout(config.timeout.toLong(), TimeUnit.SECONDS)
        .writeTimeout(config.timeout.toLong(), TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(config))
        .addInterceptor(LoggingInterceptor())
        .build()

    private val baseUrl = config.baseUrl.trimEnd('/')
    private val apiPath = "/rest/api"

    // ========== Space Operations ==========

    override suspend fun getSpace(spaceKey: String): Space {
        val response = executeWithRetry {
            get("$apiPath/space/$spaceKey")
        }
        return json.decodeFromString(Space.serializer(), response)
    }

    // ========== Page Operations ==========

    override suspend fun getPage(pageId: String, expand: List<String>): Page {
        val expandParam = if (expand.isNotEmpty()) "?expand=${expand.joinToString(",")}" else ""
        val response = executeWithRetry {
            get("$apiPath/content/$pageId$expandParam")
        }
        return json.decodeFromString(Page.serializer(), response)
    }

    override suspend fun getPageByTitle(spaceKey: String, title: String): Page? {
        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
        val path = "$apiPath/content?spaceKey=$spaceKey&title=$encodedTitle&expand=version,body.storage,ancestors"

        return try {
            val response = executeWithRetry { get(path) }
            val contentResponse = json.decodeFromString(ContentResponse.serializer(), response)
            contentResponse.results.firstOrNull()
        } catch (e: NotFoundException) {
            null
        }
    }

    override suspend fun createPage(request: CreatePageRequest): Page {
        val body = json.encodeToString(CreatePageRequest.serializer(), request)
        val response = executeWithRetry {
            post("$apiPath/content", body)
        }
        return json.decodeFromString(Page.serializer(), response)
    }

    override suspend fun updatePage(pageId: String, request: UpdatePageRequest): Page {
        val body = json.encodeToString(UpdatePageRequest.serializer(), request)
        val response = executeWithRetry {
            put("$apiPath/content/$pageId", body)
        }
        return json.decodeFromString(Page.serializer(), response)
    }

    override suspend fun deletePage(pageId: String) {
        executeWithRetry {
            delete("$apiPath/content/$pageId")
        }
    }

    // ========== Hierarchy Operations ==========

    override suspend fun getChildPages(
        pageId: String,
        expand: List<String>,
        limit: Int
    ): List<Page> {
        val expandParam = if (expand.isNotEmpty()) "&expand=${expand.joinToString(",")}" else ""
        val path = "$apiPath/content/$pageId/child/page?limit=$limit$expandParam"

        val response = executeWithRetry { get(path) }
        val contentResponse = json.decodeFromString(ContentResponse.serializer(), response)
        return contentResponse.results
    }

    override suspend fun getDescendantPages(
        pageId: String,
        expand: List<String>
    ): List<Page> {
        val allDescendants = mutableListOf<Page>()
        val toProcess = ArrayDeque<String>()
        toProcess.add(pageId)

        while (toProcess.isNotEmpty()) {
            val currentId = toProcess.removeFirst()
            val children = getChildPages(currentId, expand, 100)
            allDescendants.addAll(children)
            children.forEach { toProcess.add(it.id) }
        }

        return allDescendants
    }

    override suspend fun getAncestors(pageId: String): List<Page> {
        val page = getPage(pageId, listOf("ancestors"))
        return page.ancestors?.map { ref ->
            getPage(ref.id, listOf("version"))
        } ?: emptyList()
    }

    override suspend fun movePage(pageId: String, newParentId: String): Page {
        // Get current page to preserve content
        val currentPage = getPage(pageId, listOf("body.storage", "version", "space"))

        val request = UpdatePageRequest(
            id = pageId,
            type = "page",
            title = currentPage.title,
            space = currentPage.space ?: throw ValidationException("Page has no space"),
            body = currentPage.body ?: Body(storage = Storage("")),
            version = VersionUpdate(
                number = (currentPage.version?.number ?: 0) + 1,
                message = "Moved page"
            ),
            ancestors = listOf(PageReference(id = newParentId))
        )

        return updatePage(pageId, request)
    }

    // ========== Search Operations ==========

    override suspend fun search(
        cql: String,
        limit: Int,
        start: Int,
        expand: List<String>
    ): SearchResult {
        val encodedCql = java.net.URLEncoder.encode(cql, "UTF-8")
        val expandParam = if (expand.isNotEmpty()) "&expand=${expand.joinToString(",")}" else ""
        val path = "$apiPath/content/search?cql=$encodedCql&limit=$limit&start=$start$expandParam"

        val response = executeWithRetry { get(path) }
        return json.decodeFromString(SearchResult.serializer(), response)
    }

    override suspend fun getAllPagesInSpace(
        spaceKey: String,
        expand: List<String>,
        limit: Int
    ): List<Page> {
        val allPages = mutableListOf<Page>()
        var start = 0

        while (true) {
            val expandParam = if (expand.isNotEmpty()) "&expand=${expand.joinToString(",")}" else ""
            val path = "$apiPath/content?spaceKey=$spaceKey&type=page&limit=$limit&start=$start$expandParam"

            val response = executeWithRetry { get(path) }
            val contentResponse = json.decodeFromString(ContentResponse.serializer(), response)

            allPages.addAll(contentResponse.results)

            if (contentResponse.results.size < limit) {
                break
            }
            start += limit
        }

        return allPages
    }

    // ========== Attachment Operations ==========

    override suspend fun getAttachments(pageId: String, limit: Int): List<Attachment> {
        val path = "$apiPath/content/$pageId/child/attachment?limit=$limit"
        val response = executeWithRetry { get(path) }
        val attachmentResponse = json.decodeFromString(AttachmentResponse.serializer(), response)
        return attachmentResponse.results
    }

    override suspend fun uploadAttachment(
        pageId: String,
        file: File,
        comment: String?
    ): Attachment {
        val response = executeWithRetry {
            uploadFile("$apiPath/content/$pageId/child/attachment", file, comment)
        }
        val attachmentResponse = json.decodeFromString(AttachmentResponse.serializer(), response)
        return attachmentResponse.results.first()
    }

    override suspend fun updateAttachment(
        pageId: String,
        attachmentId: String,
        file: File,
        comment: String?
    ): Attachment {
        val response = executeWithRetry {
            uploadFile("$apiPath/content/$pageId/child/attachment/$attachmentId/data", file, comment)
        }
        val attachmentResponse = json.decodeFromString(AttachmentResponse.serializer(), response)
        return attachmentResponse.results.first()
    }

    // ========== Utility Operations ==========

    override suspend fun testConnection(): Boolean {
        return try {
            // Try to get current user info
            executeWithRetry { get("$apiPath/user/current") }
            true
        } catch (e: AuthenticationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Connection test failed", e)
            false
        }
    }

    override fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    // ========== Private HTTP Methods ==========

    private fun get(path: String): String {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .get()
            .build()
        return executeRequest(request)
    }

    private fun post(path: String, body: String): String {
        val requestBody = body.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(requestBody)
            .build()
        return executeRequest(request)
    }

    private fun put(path: String, body: String): String {
        val requestBody = body.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl$path")
            .put(requestBody)
            .build()
        return executeRequest(request)
    }

    private fun delete(path: String): String {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .delete()
            .build()
        return executeRequest(request)
    }

    private fun uploadFile(path: String, file: File, comment: String?): String {
        val mediaType = guessMediaType(file.name)
        val fileBody = file.asRequestBody(mediaType.toMediaType())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, fileBody)
            .apply {
                if (comment != null) {
                    addFormDataPart("comment", comment)
                }
            }
            .build()

        val request = Request.Builder()
            .url("$baseUrl$path")
            .header("X-Atlassian-Token", "nocheck")
            .post(multipartBody)
            .build()

        return executeRequest(request)
    }

    private fun executeRequest(request: Request): String {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                when (response.code) {
                    200, 201, 204 -> return body
                    401 -> throw AuthenticationException()
                    403 -> throw ForbiddenException(
                        message = "Access forbidden: ${request.url}",
                        resource = request.url.toString()
                    )
                    404 -> throw NotFoundException(
                        message = "Resource not found: ${request.url}",
                        resourceId = request.url.pathSegments.lastOrNull()
                    )
                    409 -> throw ConflictException(
                        message = "Conflict: $body"
                    )
                    429 -> {
                        val retryAfter = response.header("Retry-After")?.toIntOrNull()
                        throw RateLimitException(
                            retryAfterSeconds = retryAfter
                        )
                    }
                    in 500..599 -> throw ServerException(
                        message = "Server error: $body",
                        statusCode = response.code
                    )
                    else -> throw ConfluenceException(
                        "Unexpected response code ${response.code}: $body"
                    )
                }
            }
        } catch (e: IOException) {
            throw NetworkException("Network error: ${e.message}", e)
        }
    }

    private suspend fun executeWithRetry(block: () -> String): String {
        var lastException: Exception? = null

        for (attempt in 0 until config.retryCount) {
            try {
                return block()
            } catch (e: RateLimitException) {
                val waitTime = e.retryAfterSeconds?.toLong() ?: (2.0.pow(attempt.toDouble()).toLong())
                logger.warn("Rate limited. Waiting ${waitTime}s before retry (attempt ${attempt + 1}/${config.retryCount})")
                delay(waitTime * 1000)
                lastException = e
            } catch (e: NetworkException) {
                val waitTime = 2.0.pow(attempt.toDouble()).toLong()
                logger.warn("Network error. Waiting ${waitTime}s before retry (attempt ${attempt + 1}/${config.retryCount})")
                delay(waitTime * 1000)
                lastException = e
            } catch (e: ServerException) {
                val waitTime = 2.0.pow(attempt.toDouble()).toLong()
                logger.warn("Server error (${e.statusCode}). Waiting ${waitTime}s before retry (attempt ${attempt + 1}/${config.retryCount})")
                delay(waitTime * 1000)
                lastException = e
            } catch (e: Exception) {
                // Non-retryable exceptions
                throw e
            }
        }

        throw MaxRetriesExceededException(
            message = "Max retries exceeded after ${config.retryCount} attempts",
            attempts = config.retryCount,
            cause = lastException
        )
    }

    private fun guessMediaType(filename: String): String {
        return when (filename.substringAfterLast('.').lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            "md" -> "text/markdown"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}

/**
 * OkHttp interceptor for adding authentication headers.
 */
private class AuthInterceptor(private val config: ConfluenceConfig) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val authenticatedRequest = originalRequest.newBuilder()
            .apply {
                if (!config.pat.isNullOrBlank()) {
                    // Personal Access Token (Data Center/Server)
                    header("Authorization", "Bearer ${config.pat}")
                } else if (!config.username.isNullOrBlank() && !config.apiToken.isNullOrBlank()) {
                    // Basic Auth with API Token (Cloud)
                    val credentials = "${config.username}:${config.apiToken}"
                    val encoded = java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
                    header("Authorization", "Basic $encoded")
                }
                header("Accept", "application/json")
                header("Content-Type", "application/json")
            }
            .build()

        return chain.proceed(authenticatedRequest)
    }
}

/**
 * OkHttp interceptor for request/response logging.
 */
private class LoggingInterceptor : Interceptor {
    private val logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        logger.debug(">>> ${request.method} ${request.url}")

        val response = chain.proceed(request)

        val duration = System.currentTimeMillis() - startTime
        logger.debug("<<< ${response.code} ${request.url} (${duration}ms)")

        return response
    }
}
