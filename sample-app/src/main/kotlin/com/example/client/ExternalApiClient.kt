package com.example.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.MDC

class ExternalApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {

    suspend fun fetchData(): HttpResponse {
        return httpClient.get("$baseUrl/external/data")
    }
}
