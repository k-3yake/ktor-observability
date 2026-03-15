package com.example

import com.example.routes.ProxyResponse
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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

    private suspend fun callProxyAndCapture(): Pair<ProxyResponse, com.github.tomakehurst.wiremock.verification.LoggedRequest> {
        wireMockServer.resetRequests()
        val responseBody = testClient.get("http://localhost:$serverPort/api/proxy").bodyAsText()
        val proxyResponse = Json.decodeFromString<ProxyResponse>(responseBody)
        val captured = wireMockServer.findAll(getRequestedFor(urlEqualTo("/external/data")))
        assertEquals(1, captured.size, "Expected exactly one request to /external/data")
        return proxyResponse to captured[0]
    }

    @Test
    fun `x-datadog-trace-id is injected and matches the active trace`() = runBlocking {
        val (proxyResponse, captured) = callProxyAndCapture()

        val headerTraceId = captured.getHeader("x-datadog-trace-id")
        assertEquals(
            proxyResponse.traceId, headerTraceId,
            "x-datadog-trace-id header should match the active trace's ID from MDC"
        )
    }

    @Test
    fun `x-datadog-parent-id is injected as a child span of the active span`() = runBlocking {
        val (proxyResponse, captured) = callProxyAndCapture()

        val headerParentId = captured.getHeader("x-datadog-parent-id")
        assertNotEquals(
            "0", headerParentId,
            "x-datadog-parent-id should not be zero"
        )
        assertNotEquals(
            proxyResponse.traceId, headerParentId,
            "x-datadog-parent-id should differ from trace-id (it is a span-id)"
        )
    }

    @Test
    fun `x-datadog-sampling-priority is injected with auto-keep`() = runBlocking {
        val (_, captured) = callProxyAndCapture()

        val samplingPriority = captured.getHeader("x-datadog-sampling-priority")
        assertEquals(
            "1", samplingPriority,
            "x-datadog-sampling-priority should be 1 (AUTO_KEEP) with default sample_rate=1"
        )
    }
}
