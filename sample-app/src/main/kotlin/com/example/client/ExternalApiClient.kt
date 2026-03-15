package com.example.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.MDC

class ExternalApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    var lastTraceId: String? = null
        private set
    var lastSpanId: String? = null
        private set

    suspend fun fetchData(): HttpResponse {
        lastTraceId = MDC.get("dd.trace_id")
        lastSpanId = MDC.get("dd.span_id")
        return httpClient.get("$baseUrl/external/data")
    }
}
