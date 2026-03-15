package com.example

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TraceContextPropagationTest {

    private lateinit var wireMockServer: WireMockServer
    private lateinit var testClient: HttpClient
    private var serverPort: Int = 0

    private val ktorServer by lazy {
        embeddedServer(Netty, port = 0) {
            module(externalApiBaseUrl = "http://localhost:${wireMockServer.port()}")
        }
    }

    @BeforeAll
    fun setUp() {
        wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
        wireMockServer.start()
        wireMockServer.stubFor(
            get(urlEqualTo("/external/data"))
                .willReturn(aResponse().withStatus(200))
        )

        ktorServer.start(wait = false)
        serverPort = runBlocking { ktorServer.resolvedConnectors() }.first().port

        testClient = HttpClient(CIO)
    }

    @AfterAll
    fun tearDown() {
        testClient.close()
        ktorServer.stop(1000, 2000)
        wireMockServer.stop()
    }

    @Test
    fun `x-datadog-trace-id is injected into outgoing HTTP request`() = runBlocking {
        wireMockServer.resetRequests()

        testClient.get("http://localhost:$serverPort/api/proxy")

        val requests = wireMockServer.findAll(getRequestedFor(urlEqualTo("/external/data")))
        assertEquals(1, requests.size, "Expected exactly one request to /external/data")

        val traceId = requests[0].getHeader("x-datadog-trace-id")
        assertNotNull(traceId, "x-datadog-trace-id header should be present")
        assertTrue(traceId.isNotBlank(), "x-datadog-trace-id header should not be blank")
        assertDoesNotThrow({ traceId.toLong() }, "x-datadog-trace-id should be a numeric string")
    }

    @Test
    fun `x-datadog-parent-id is injected into outgoing HTTP request`() = runBlocking {
        wireMockServer.resetRequests()

        testClient.get("http://localhost:$serverPort/api/proxy")

        val requests = wireMockServer.findAll(getRequestedFor(urlEqualTo("/external/data")))
        assertEquals(1, requests.size, "Expected exactly one request to /external/data")

        val parentId = requests[0].getHeader("x-datadog-parent-id")
        assertNotNull(parentId, "x-datadog-parent-id header should be present")
        assertTrue(parentId.isNotBlank(), "x-datadog-parent-id header should not be blank")
        assertDoesNotThrow({ parentId.toLong() }, "x-datadog-parent-id should be a numeric string")
    }

    @Test
    fun `x-datadog-sampling-priority is injected into outgoing HTTP request`() = runBlocking {
        wireMockServer.resetRequests()

        testClient.get("http://localhost:$serverPort/api/proxy")

        val requests = wireMockServer.findAll(getRequestedFor(urlEqualTo("/external/data")))
        assertEquals(1, requests.size, "Expected exactly one request to /external/data")

        val samplingPriority = requests[0].getHeader("x-datadog-sampling-priority")
        assertNotNull(samplingPriority, "x-datadog-sampling-priority header should be present")
        assertTrue(
            samplingPriority in listOf("-1", "0", "1", "2"),
            "x-datadog-sampling-priority should be one of -1, 0, 1, 2 but was: $samplingPriority"
        )
    }
}
