package com.example

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
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
import org.slf4j.LoggerFactory

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
        serverPort = runBlocking { ktorServer.engine.resolvedConnectors() }.first().port

        testClient = HttpClient(CIO)
    }

    @AfterAll
    fun tearDown() {
        testClient.close()
        ktorServer.stop(1000, 2000)
        wireMockServer.stop()
    }

    private suspend fun callProxyWithTraceHeaders(
        traceId: String,
        parentId: String,
        samplingPriority: String
    ): com.github.tomakehurst.wiremock.verification.LoggedRequest {
        wireMockServer.resetRequests()
        testClient.get("http://localhost:$serverPort/api/proxy") {
            headers {
                append("x-datadog-trace-id", traceId)
                append("x-datadog-parent-id", parentId)
                append("x-datadog-sampling-priority", samplingPriority)
            }
        }
        val captured = wireMockServer.findAll(getRequestedFor(urlEqualTo("/external/data")))
        assertEquals(1, captured.size, "Expected exactly one request to /external/data")
        return captured[0]
    }

    @Test
    fun `x-datadog-trace-id is propagated through withContext(Dispatchers_IO)`() = runBlocking {
        val captured = callProxyWithTraceHeaders(
            traceId = "123456789",
            parentId = "987654321",
            samplingPriority = "2"
        )

        assertEquals(
            "123456789", captured.getHeader("x-datadog-trace-id"),
            "x-datadog-trace-id should be propagated from incoming request to outgoing request"
        )
    }

    @Test
    fun `x-datadog-parent-id is propagated through withContext(Dispatchers_IO)`() = runBlocking {
        val captured = callProxyWithTraceHeaders(
            traceId = "123456789",
            parentId = "987654321",
            samplingPriority = "2"
        )

        val headerParentId = captured.getHeader("x-datadog-parent-id")
        val parentIdValue = assertDoesNotThrow({ headerParentId.toLong() },
            "x-datadog-parent-id should be a numeric string"
        )
        assertNotEquals(
            0L, parentIdValue,
            "x-datadog-parent-id should not be zero"
        )
    }

    @Test
    fun `x-datadog-sampling-priority is propagated through withContext(Dispatchers_IO)`() = runBlocking {
        val captured = callProxyWithTraceHeaders(
            traceId = "123456789",
            parentId = "987654321",
            samplingPriority = "2"
        )

        assertEquals(
            "2", captured.getHeader("x-datadog-sampling-priority"),
            "x-datadog-sampling-priority should be propagated from incoming request to outgoing request"
        )
    }

    @Test
    fun `dd_parent_id is set in MDC during request processing`() = runBlocking {
        val listAppender = ListAppender<ILoggingEvent>()
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        listAppender.start()
        rootLogger.addAppender(listAppender)

        try {
            testClient.get("http://localhost:$serverPort/api/proxy") {
                headers {
                    append("x-datadog-trace-id", "123456789")
                    append("x-datadog-parent-id", "987654321")
                    append("x-datadog-sampling-priority", "2")
                }
            }

            val parentIds = listAppender.list.mapNotNull { it.mdcPropertyMap["dd.parent_id"] }
            assertTrue(parentIds.isNotEmpty(), "dd.parent_id should be present in MDC of log events")
            assertTrue(
                parentIds.all { it.toLongOrNull() != null && it.toLong() != 0L },
                "dd.parent_id should be a non-zero numeric string, but was: $parentIds"
            )
        } finally {
            rootLogger.detachAppender(listAppender)
            listAppender.stop()
        }
    }
}
