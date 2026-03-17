package com.example.routes

import com.example.module
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRouteTest {

    private lateinit var testClient: HttpClient
    private var serverPort: Int = 0

    private val ktorServer by lazy {
        embeddedServer(Netty, port = 0) {
            module()
        }
    }

    @BeforeAll
    fun setUp() {
        ktorServer.start(wait = false)
        serverPort = runBlocking { ktorServer.engine.resolvedConnectors() }.first().port
        testClient = HttpClient(CIO)
    }

    @AfterAll
    fun tearDown() {
        testClient.close()
        ktorServer.stop(1000, 2000)
    }

    @Test
    fun `POST api_users with invalid JSON returns 400`() = runBlocking {
        val response = testClient.post("http://localhost:$serverPort/api/users") {
            contentType(ContentType.Application.Json)
            setBody("""{"invalid": "json"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST api_users with non-JSON body returns 400`() = runBlocking {
        val response = testClient.post("http://localhost:$serverPort/api/users") {
            contentType(ContentType.Application.Json)
            setBody("not json at all")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
