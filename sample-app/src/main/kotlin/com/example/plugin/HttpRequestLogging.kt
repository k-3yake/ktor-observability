package com.example.plugin

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.LoggerFactory

val HttpRequestLogging = createApplicationPlugin(name = "HttpRequestLogging") {
    val logger = LoggerFactory.getLogger("HttpRequestLogging")

    on(ResponseSent) { call ->
        val httpRequest = mapOf(
            "requestMethod" to call.request.httpMethod.value,
            "requestUrl" to call.request.uri,
            "status" to (call.response.status()?.value ?: 0),
            "latency" to "${call.processingTimeMillis() / 1000.0}s"
        )
        logger.info("{}", StructuredArguments.value("httpRequest", httpRequest))
    }
}
