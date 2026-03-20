package com.example

import com.example.client.ExternalApiClient
import com.example.repository.UserRepository
import com.example.routes.ErrorResponse
import com.example.routes.proxyRoute
import com.example.routes.userRoute
import com.example.service.UserService
import com.example.validator.UserValidator
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugin.HttpRequestLogging
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import org.slf4j.event.Level
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module(externalApiBaseUrl: String = "http://localhost:9090"): ExternalApiClient {
    intercept(ApplicationCallPipeline.Monitoring) {
        val parentId = call.request.headers["x-datadog-parent-id"] ?: "0"
        org.slf4j.MDC.putCloseable("dd.parent_id", parentId).use { proceed() }
    }

    install(CallLogging) {
        disableDefaultColors()
        level = Level.INFO
        filter { false }
        mdc("method") { it.request.httpMethod.value }
        mdc("path") { it.request.uri }
        mdc("status") { it.response.status()?.value?.toString() }
        mdc("duration") { it.processingTimeMillis().toString() }
    }
    install(HttpRequestLogging)
    install(ContentNegotiation) {
        json()
    }
    val log = log
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            log.warn("Request deserialization failed: ${cause.message}", cause)
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad Request"))
        }
    }

    val httpClient = HttpClient(Java)
    val externalApiClient = ExternalApiClient(httpClient, externalApiBaseUrl)
    val userRepository = UserRepository()
    val userValidator = UserValidator()
    val userService = UserService(userRepository)

    routing {
        proxyRoute(externalApiClient)
        userRoute(userValidator, userService)
    }

    return externalApiClient
}
