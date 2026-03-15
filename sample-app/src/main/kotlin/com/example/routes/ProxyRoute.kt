package com.example.routes

import com.example.client.ExternalApiClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class ProxyResponse(
    val traceId: String,
    val spanId: String
)

fun Route.proxyRoute(externalApiClient: ExternalApiClient) {
    get("/api/proxy") {
        // Force thread switch with Dispatchers.IO to test coroutine context propagation
        withContext(Dispatchers.IO) {
            externalApiClient.fetchData()
        }

        call.respond(HttpStatusCode.OK)
    }
}
