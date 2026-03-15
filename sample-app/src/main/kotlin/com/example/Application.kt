package com.example

import com.example.client.ExternalApiClient
import com.example.routes.proxyRoute
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module(externalApiBaseUrl: String = "http://localhost:9090"): ExternalApiClient {
    install(ContentNegotiation) {
        json()
    }

    val httpClient = HttpClient(Java)
    val externalApiClient = ExternalApiClient(httpClient, externalApiBaseUrl)

    routing {
        proxyRoute(externalApiClient)
    }

    return externalApiClient
}
