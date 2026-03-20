package com.example.routes

import com.example.client.ExternalApiClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
data class ProxyResponse(
    val traceId: String,
    val spanId: String
)

private val logger = LoggerFactory.getLogger("com.example.routes.ProxyRoute")

fun Route.proxyRoute(externalApiClient: ExternalApiClient) {
    get("/api/proxy") {
        logger.info("Before IO switch")
        withContext(Dispatchers.IO) {
            logger.info("Inside IO switch")
            externalApiClient.fetchData()
        }
        logger.info("After IO switch")

        call.respond(HttpStatusCode.OK)
    }
}
