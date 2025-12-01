package com.consync.client.confluence

import com.consync.client.confluence.exception.*
import com.consync.client.confluence.model.*
import com.consync.config.ConfluenceConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for ConfluenceClientImpl.
 *
 * Note: These tests use a mock server (WireMock) to simulate Confluence API responses.
 * For integration tests with a real Confluence instance, see ConfluenceClientIntegrationTest.
 */
class ConfluenceClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Helper to create test config
    private fun testConfig(baseUrl: String = "http://localhost:8080") = ConfluenceConfig(
        baseUrl = baseUrl,
        username = "test@example.com",
        apiToken = "test-token",
        timeout = 10,
        retryCount = 1
    )

    @Test
    fun `should serialize CreatePageRequest correctly`() {
        val request = CreatePageRequest(
            title = "Test Page",
            space = SpaceReference(key = "DOCS"),
            body = Body(
                storage = Storage(
                    value = "<p>Hello World</p>",
                    representation = "storage"
                )
            ),
            ancestors = listOf(PageReference(id = "123456"))
        )

        val serialized = json.encodeToString(CreatePageRequest.serializer(), request)

        assert(serialized.contains("\"title\":\"Test Page\""))
        assert(serialized.contains("\"key\":\"DOCS\""))
        assert(serialized.contains("<p>Hello World</p>"))
        assert(serialized.contains("\"id\":\"123456\""))
    }

    @Test
    fun `should serialize UpdatePageRequest correctly`() {
        val request = UpdatePageRequest(
            id = "789",
            title = "Updated Page",
            space = SpaceReference(key = "DOCS"),
            body = Body(
                storage = Storage(
                    value = "<p>Updated content</p>",
                    representation = "storage"
                )
            ),
            version = VersionUpdate(
                number = 2,
                minorEdit = false,
                message = "Updated via API"
            )
        )

        val serialized = json.encodeToString(UpdatePageRequest.serializer(), request)

        assert(serialized.contains("\"id\":\"789\""))
        assert(serialized.contains("\"number\":2"))
        assert(serialized.contains("\"message\":\"Updated via API\""))
    }

    @Test
    fun `should deserialize Page response correctly`() {
        val pageJson = """
            {
                "id": "123456",
                "type": "page",
                "status": "current",
                "title": "My Page",
                "space": {
                    "key": "DOCS",
                    "id": 1,
                    "name": "Documentation"
                },
                "version": {
                    "number": 5,
                    "minorEdit": false,
                    "by": {
                        "displayName": "John Doe"
                    }
                },
                "body": {
                    "storage": {
                        "value": "<p>Content here</p>",
                        "representation": "storage"
                    }
                },
                "ancestors": [
                    {"id": "111", "title": "Parent Page"}
                ],
                "_links": {
                    "webui": "/display/DOCS/My+Page",
                    "self": "http://confluence/rest/api/content/123456"
                }
            }
        """.trimIndent()

        val page = json.decodeFromString(Page.serializer(), pageJson)

        assertEquals("123456", page.id)
        assertEquals("page", page.type)
        assertEquals("My Page", page.title)
        assertEquals("DOCS", page.space?.key)
        assertEquals(5, page.version?.number)
        assertEquals("<p>Content here</p>", page.body?.storage?.value)
        assertEquals(1, page.ancestors?.size)
        assertEquals("111", page.ancestors?.first()?.id)
    }

    @Test
    fun `should deserialize Space response correctly`() {
        val spaceJson = """
            {
                "id": 12345,
                "key": "DOCS",
                "name": "Documentation Space",
                "type": "global",
                "status": "current",
                "_links": {
                    "webui": "/display/DOCS",
                    "self": "http://confluence/rest/api/space/DOCS"
                }
            }
        """.trimIndent()

        val space = json.decodeFromString(Space.serializer(), spaceJson)

        assertEquals(12345, space.id)
        assertEquals("DOCS", space.key)
        assertEquals("Documentation Space", space.name)
        assertEquals("global", space.type)
    }

    @Test
    fun `should deserialize ContentResponse correctly`() {
        val responseJson = """
            {
                "results": [
                    {"id": "1", "type": "page", "title": "Page 1", "status": "current"},
                    {"id": "2", "type": "page", "title": "Page 2", "status": "current"}
                ],
                "start": 0,
                "limit": 25,
                "size": 2,
                "_links": {
                    "self": "http://confluence/rest/api/content"
                }
            }
        """.trimIndent()

        val response = json.decodeFromString(ContentResponse.serializer(), responseJson)

        assertEquals(2, response.results.size)
        assertEquals("1", response.results[0].id)
        assertEquals("Page 1", response.results[0].title)
        assertEquals(0, response.start)
        assertEquals(25, response.limit)
    }

    @Test
    fun `should deserialize SearchResult correctly`() {
        val searchJson = """
            {
                "results": [
                    {
                        "content": {
                            "id": "123",
                            "type": "page",
                            "title": "Found Page",
                            "status": "current"
                        },
                        "title": "Found Page",
                        "excerpt": "...matching content...",
                        "url": "/display/DOCS/Found+Page"
                    }
                ],
                "start": 0,
                "limit": 25,
                "size": 1,
                "totalSize": 1
            }
        """.trimIndent()

        val result = json.decodeFromString(SearchResult.serializer(), searchJson)

        assertEquals(1, result.results.size)
        assertEquals("123", result.results[0].content?.id)
        assertEquals("Found Page", result.results[0].title)
        assertEquals(1, result.totalSize)
    }

    @Test
    fun `should deserialize Attachment correctly`() {
        val attachmentJson = """
            {
                "id": "att123",
                "type": "attachment",
                "status": "current",
                "title": "image.png",
                "version": {
                    "number": 1
                },
                "extensions": {
                    "mediaType": "image/png",
                    "fileSize": 12345,
                    "comment": "Uploaded image"
                },
                "_links": {
                    "download": "/download/attachments/123/image.png",
                    "self": "http://confluence/rest/api/content/att123"
                }
            }
        """.trimIndent()

        val attachment = json.decodeFromString(Attachment.serializer(), attachmentJson)

        assertEquals("att123", attachment.id)
        assertEquals("image.png", attachment.title)
        assertEquals("image/png", attachment.extensions?.mediaType)
        assertEquals(12345, attachment.extensions?.fileSize)
    }

    @Test
    fun `should handle missing optional fields`() {
        val minimalPageJson = """
            {
                "id": "123",
                "title": "Minimal Page"
            }
        """.trimIndent()

        val page = json.decodeFromString(Page.serializer(), minimalPageJson)

        assertEquals("123", page.id)
        assertEquals("Minimal Page", page.title)
        assertNull(page.space)
        assertNull(page.version)
        assertNull(page.body)
        assertNull(page.ancestors)
    }

    @Test
    fun `factory should create cloud client`() {
        val client = ConfluenceClientFactory.createCloudClient(
            baseUrl = "https://example.atlassian.net/wiki",
            username = "user@example.com",
            apiToken = "token123"
        )

        assertNotNull(client)
        client.close()
    }

    @Test
    fun `factory should create server client`() {
        val client = ConfluenceClientFactory.createServerClient(
            baseUrl = "https://confluence.company.com",
            pat = "pat-token"
        )

        assertNotNull(client)
        client.close()
    }
}

/**
 * Tests for exception classes.
 */
class ConfluenceExceptionTest {

    @Test
    fun `AuthenticationException should have default message`() {
        val exception = AuthenticationException()
        assert(exception.message!!.contains("Authentication"))
    }

    @Test
    fun `NotFoundException should contain resource info`() {
        val exception = NotFoundException(
            message = "Page not found",
            resourceType = "page",
            resourceId = "12345"
        )

        assertEquals("Page not found", exception.message)
        assertEquals("page", exception.resourceType)
        assertEquals("12345", exception.resourceId)
    }

    @Test
    fun `ConflictException should contain version info`() {
        val exception = ConflictException(
            message = "Version conflict",
            pageId = "123",
            expectedVersion = 5,
            actualVersion = 6
        )

        assertEquals("123", exception.pageId)
        assertEquals(5, exception.expectedVersion)
        assertEquals(6, exception.actualVersion)
    }

    @Test
    fun `RateLimitException should contain retry info`() {
        val exception = RateLimitException(
            retryAfterSeconds = 30
        )

        assertEquals(30, exception.retryAfterSeconds)
    }

    @Test
    fun `MaxRetriesExceededException should contain attempt count`() {
        val exception = MaxRetriesExceededException(
            attempts = 3
        )

        assertEquals(3, exception.attempts)
    }
}
