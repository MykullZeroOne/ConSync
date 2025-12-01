package com.consync.client.confluence

import com.consync.client.confluence.exception.*
import com.consync.client.confluence.model.*
import com.consync.config.ConfluenceConfig
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for ConfluenceClientImpl using WireMock.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfluenceClientWireMockTest {

    private lateinit var wireMockServer: WireMockServer
    private lateinit var client: ConfluenceClient

    @BeforeAll
    fun setupServer() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()
    }

    @AfterAll
    fun teardownServer() {
        wireMockServer.stop()
    }

    @BeforeEach
    fun setup() {
        wireMockServer.resetAll()

        val config = ConfluenceConfig(
            baseUrl = "http://localhost:${wireMockServer.port()}",
            username = "test@example.com",
            apiToken = "test-token",
            timeout = 10,
            retryCount = 2
        )
        client = ConfluenceClientImpl(config)
    }

    @AfterEach
    fun cleanup() {
        client.close()
    }

    // ========== Space Operations ==========

    @Test
    fun `getSpace should return space info`() = runBlocking {
        wireMockServer.stubFor(
            get(urlEqualTo("/rest/api/space/DOCS"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": 12345,
                                "key": "DOCS",
                                "name": "Documentation",
                                "type": "global"
                            }
                        """.trimIndent())
                )
        )

        val space = client.getSpace("DOCS")

        assertEquals(12345, space.id)
        assertEquals("DOCS", space.key)
        assertEquals("Documentation", space.name)
    }

    @Test
    fun `getSpace should throw NotFoundException for missing space`() = runBlocking {
        wireMockServer.stubFor(
            get(urlEqualTo("/rest/api/space/MISSING"))
                .willReturn(aResponse().withStatus(404))
        )

        assertThrows<NotFoundException> {
            runBlocking { client.getSpace("MISSING") }
        }
    }

    // ========== Page Operations ==========

    @Test
    fun `getPage should return page with expanded fields`() = runBlocking {
        wireMockServer.stubFor(
            get(urlEqualTo("/rest/api/content/123?expand=body.storage,version"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "123",
                                "type": "page",
                                "title": "Test Page",
                                "status": "current",
                                "version": {"number": 5},
                                "body": {
                                    "storage": {
                                        "value": "<p>Content</p>",
                                        "representation": "storage"
                                    }
                                }
                            }
                        """.trimIndent())
                )
        )

        val page = client.getPage("123", listOf("body.storage", "version"))

        assertEquals("123", page.id)
        assertEquals("Test Page", page.title)
        assertEquals(5, page.version?.number)
        assertEquals("<p>Content</p>", page.body?.storage?.value)
    }

    @Test
    fun `getPageByTitle should return page when found`() = runBlocking {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/rest/api/content"))
                .withQueryParam("spaceKey", equalTo("DOCS"))
                .withQueryParam("title", equalTo("My%20Page"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "results": [
                                    {"id": "456", "type": "page", "title": "My Page", "status": "current"}
                                ],
                                "size": 1
                            }
                        """.trimIndent())
                )
        )

        val page = client.getPageByTitle("DOCS", "My Page")

        assertNotNull(page)
        assertEquals("456", page?.id)
        assertEquals("My Page", page?.title)
    }

    @Test
    fun `getPageByTitle should return null when not found`() = runBlocking {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/rest/api/content"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"results": [], "size": 0}""")
                )
        )

        val page = client.getPageByTitle("DOCS", "Nonexistent")

        assertNull(page)
    }

    @Test
    fun `createPage should create and return new page`() = runBlocking {
        wireMockServer.stubFor(
            post(urlEqualTo("/rest/api/content"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "789",
                                "type": "page",
                                "title": "New Page",
                                "status": "current",
                                "version": {"number": 1}
                            }
                        """.trimIndent())
                )
        )

        val request = CreatePageRequest(
            title = "New Page",
            space = SpaceReference(key = "DOCS"),
            body = Body(storage = Storage(value = "<p>Hello</p>"))
        )

        val page = client.createPage(request)

        assertEquals("789", page.id)
        assertEquals("New Page", page.title)
        assertEquals(1, page.version?.number)
    }

    @Test
    fun `updatePage should update and return page`() = runBlocking {
        wireMockServer.stubFor(
            put(urlEqualTo("/rest/api/content/123"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "123",
                                "type": "page",
                                "title": "Updated Page",
                                "status": "current",
                                "version": {"number": 2}
                            }
                        """.trimIndent())
                )
        )

        val request = UpdatePageRequest(
            id = "123",
            title = "Updated Page",
            space = SpaceReference(key = "DOCS"),
            body = Body(storage = Storage(value = "<p>Updated</p>")),
            version = VersionUpdate(number = 2)
        )

        val page = client.updatePage("123", request)

        assertEquals("123", page.id)
        assertEquals("Updated Page", page.title)
        assertEquals(2, page.version?.number)
    }

    @Test
    fun `deletePage should succeed with 204`() = runBlocking {
        wireMockServer.stubFor(
            delete(urlEqualTo("/rest/api/content/123"))
                .willReturn(aResponse().withStatus(204))
        )

        // Should not throw
        client.deletePage("123")
    }

    // ========== Error Handling ==========

    @Test
    fun `should throw AuthenticationException on 401`() {
        wireMockServer.stubFor(
            get(urlEqualTo("/rest/api/space/DOCS"))
                .willReturn(aResponse().withStatus(401))
        )

        assertThrows<AuthenticationException> {
            runBlocking { client.getSpace("DOCS") }
        }
    }

    @Test
    fun `should throw ForbiddenException on 403`() {
        wireMockServer.stubFor(
            get(urlEqualTo("/rest/api/space/PRIVATE"))
                .willReturn(aResponse().withStatus(403))
        )

        assertThrows<ForbiddenException> {
            runBlocking { client.getSpace("PRIVATE") }
        }
    }

    @Test
    fun `should throw ConflictException on 409`() {
        wireMockServer.stubFor(
            put(urlEqualTo("/rest/api/content/123"))
                .willReturn(
                    aResponse()
                        .withStatus(409)
                        .withBody("Version conflict")
                )
        )

        val request = UpdatePageRequest(
            id = "123",
            title = "Page",
            space = SpaceReference(key = "DOCS"),
            body = Body(storage = Storage(value = "")),
            version = VersionUpdate(number = 2)
        )

        assertThrows<ConflictException> {
            runBlocking { client.updatePage("123", request) }
        }
    }

    @Test
    fun `should throw ServerException on 500`() {
        wireMockServer.stubFor(
            get(urlEqualTo("/rest/api/space/DOCS"))
                .willReturn(aResponse().withStatus(500))
        )

        val exception = assertThrows<MaxRetriesExceededException> {
            runBlocking { client.getSpace("DOCS") }
        }
        assertEquals(2, exception.attempts)
    }

    // ========== Search Operations ==========

    @Test
    fun `search should return results`() = runBlocking {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/rest/api/content/search"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "results": [
                                    {
                                        "content": {"id": "1", "type": "page", "title": "Result 1", "status": "current"},
                                        "title": "Result 1"
                                    }
                                ],
                                "start": 0,
                                "limit": 25,
                                "size": 1,
                                "totalSize": 1
                            }
                        """.trimIndent())
                )
        )

        val results = client.search("type=page AND space=DOCS")

        assertEquals(1, results.results.size)
        assertEquals("Result 1", results.results[0].title)
    }

    // ========== Child Pages ==========

    @Test
    fun `getChildPages should return children`() = runBlocking {
        wireMockServer.stubFor(
            get(urlPathEqualTo("/rest/api/content/123/child/page"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "results": [
                                    {"id": "456", "type": "page", "title": "Child 1", "status": "current"},
                                    {"id": "789", "type": "page", "title": "Child 2", "status": "current"}
                                ],
                                "size": 2
                            }
                        """.trimIndent())
                )
        )

        val children = client.getChildPages("123")

        assertEquals(2, children.size)
        assertEquals("Child 1", children[0].title)
        assertEquals("Child 2", children[1].title)
    }

    // ========== Authentication ==========

    @Test
    fun `should send Basic auth header for cloud config`() = runBlocking {
        wireMockServer.stubFor(
            get(urlEqualTo("/rest/api/space/DOCS"))
                .withHeader("Authorization", matching("Basic .*"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{"id": 1, "key": "DOCS", "name": "Test"}""")
                )
        )

        client.getSpace("DOCS")

        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/rest/api/space/DOCS"))
                .withHeader("Authorization", matching("Basic .*"))
        )
    }

    @Test
    fun `should send Bearer token for PAT config`() = runBlocking {
        val patConfig = ConfluenceConfig(
            baseUrl = "http://localhost:${wireMockServer.port()}",
            pat = "my-personal-token",
            timeout = 10,
            retryCount = 1
        )
        val patClient = ConfluenceClientImpl(patConfig)

        wireMockServer.stubFor(
            get(urlEqualTo("/rest/api/space/DOCS"))
                .withHeader("Authorization", equalTo("Bearer my-personal-token"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody("""{"id": 1, "key": "DOCS", "name": "Test"}""")
                )
        )

        patClient.getSpace("DOCS")

        wireMockServer.verify(
            getRequestedFor(urlEqualTo("/rest/api/space/DOCS"))
                .withHeader("Authorization", equalTo("Bearer my-personal-token"))
        )

        patClient.close()
    }
}
