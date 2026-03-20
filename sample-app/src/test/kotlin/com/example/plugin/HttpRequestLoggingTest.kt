package com.example.plugin

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.example.module
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import com.fasterxml.jackson.databind.ObjectMapper
import net.logstash.logback.marker.ObjectAppendingMarker
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.slf4j.LoggerFactory
import java.io.StringWriter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpRequestLoggingTest {

    private lateinit var testClient: HttpClient
    private var serverPort: Int = 0
    private lateinit var listAppender: ListAppender<ILoggingEvent>

    private val ktorServer by lazy {
        embeddedServer(Netty, port = 0) {
            module()
        }
    }

    @BeforeAll
    fun setUp() {
        listAppender = ListAppender()
        listAppender.start()
        (LoggerFactory.getLogger("HttpRequestLogging") as Logger).addAppender(listAppender)

        ktorServer.start(wait = false)
        serverPort = runBlocking { ktorServer.engine.resolvedConnectors() }.first().port
        testClient = HttpClient(CIO)
    }

    @AfterAll
    fun tearDown() {
        testClient.close()
        ktorServer.stop(1000, 2000)
        (LoggerFactory.getLogger("HttpRequestLogging") as Logger).detachAppender(listAppender)
        listAppender.stop()
    }

    @Test
    fun `log contains httpRequest with requestMethod on POST`() = runBlocking {
        listAppender.list.clear()

        testClient.post("http://localhost:$serverPort/api/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"test","email":"test@example.com","phoneNumber":"090-1234-5678","age":25}""")
        }

        val event = waitForHttpRequestLog()
        assertNotNull(event, "Expected a log with httpRequest")
        val httpRequest = extractHttpRequestMap(event!!)
        assertEquals("POST", httpRequest["requestMethod"])
    }

    @Test
    fun `log contains httpRequest with requestUrl`() = runBlocking {
        listAppender.list.clear()

        testClient.post("http://localhost:$serverPort/api/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"test","email":"test@example.com","phoneNumber":"090-1234-5678","age":25}""")
        }

        val event = waitForHttpRequestLog()
        assertNotNull(event, "Expected a log with httpRequest")
        val httpRequest = extractHttpRequestMap(event!!)
        assertEquals("/api/users", httpRequest["requestUrl"])
    }

    @Test
    fun `log contains httpRequest with status code`() = runBlocking {
        listAppender.list.clear()

        testClient.post("http://localhost:$serverPort/api/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"test","email":"test@example.com","phoneNumber":"090-1234-5678","age":25}""")
        }

        val event = waitForHttpRequestLog()
        assertNotNull(event, "Expected a log with httpRequest")
        val httpRequest = extractHttpRequestMap(event!!)
        assertEquals(201, httpRequest["status"])
    }

    @Test
    fun `log contains httpRequest with latency in seconds format`() = runBlocking {
        listAppender.list.clear()

        testClient.post("http://localhost:$serverPort/api/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"test","email":"test@example.com","phoneNumber":"090-1234-5678","age":25}""")
        }

        val event = waitForHttpRequestLog()
        assertNotNull(event, "Expected a log with httpRequest")
        val httpRequest = extractHttpRequestMap(event!!)
        val latency = httpRequest["latency"] as String
        assertTrue(latency.matches(Regex("\\d+\\.\\d+s")), "latency should match '0.123s' format, got: $latency")
    }

    @Test
    fun `log contains httpRequest with status 400 for bad request`() = runBlocking {
        listAppender.list.clear()

        testClient.post("http://localhost:$serverPort/api/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"invalid":"json"}""")
        }

        val event = waitForHttpRequestLog()
        assertNotNull(event, "Expected a log with httpRequest")
        val httpRequest = extractHttpRequestMap(event!!)
        assertEquals(400, httpRequest["status"])
    }

    private fun waitForHttpRequestLog(timeoutMs: Long = 2000): ILoggingEvent? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val event = listAppender.list.find { it.argumentArray?.any { arg -> arg is ObjectAppendingMarker } == true }
            if (event != null) return event
            Thread.sleep(50)
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractHttpRequestMap(event: ILoggingEvent): Map<String, Any> {
        val marker = event.argumentArray.first { it is ObjectAppendingMarker } as ObjectAppendingMarker
        assertEquals("httpRequest", marker.fieldName, "StructuredArgument field name should be 'httpRequest'")

        val mapper = ObjectMapper()
        val writer = StringWriter()
        mapper.factory.createGenerator(writer).use { gen ->
            gen.writeStartObject()
            marker.writeTo(gen)
            gen.writeEndObject()
        }
        val json = mapper.readValue(writer.toString(), Map::class.java) as Map<String, Any>
        return json["httpRequest"] as Map<String, Any>
    }
}