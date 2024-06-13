package no.nav.etterlatte.libs.ktor.ktor.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.util.AttributeKey
import net.logstash.logback.marker.Markers
import no.nav.etterlatte.libs.ktor.RESPONSE_TIME
import no.nav.etterlatte.libs.ktor.X_REQUEST_METHOD
import no.nav.etterlatte.libs.ktor.X_REQUEST_URI
import no.nav.etterlatte.libs.ktor.X_RESPONSE_CODE
import no.nav.etterlatte.libs.ktor.X_UPSTREAM_HOST
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

internal class ClientCallLogging private constructor() {
    private val logger = LoggerFactory.getLogger(ClientCallLogging::class.java)
    private val requestStartTime = AttributeKey<Long>("requestStartTime")

    companion object : HttpClientPlugin<Any, ClientCallLogging> {
        override val key: AttributeKey<ClientCallLogging> = AttributeKey("ClientCallLogging")

        override fun prepare(block: Any.() -> Unit): ClientCallLogging = ClientCallLogging()

        override fun install(
            plugin: ClientCallLogging,
            scope: HttpClient,
        ) {
            plugin.setupPreRequestHandler(scope)
            plugin.setupPostRequestLogHandler(scope)
        }
    }

    private fun setupPreRequestHandler(client: HttpClient) {
        client.sendPipeline.intercept(HttpSendPipeline.Before) {
            context.attributes.put(requestStartTime, System.nanoTime())
            return@intercept
        }
    }

    private fun setupPostRequestLogHandler(client: HttpClient) {
        client.receivePipeline.intercept(HttpReceivePipeline.After) { res ->
            with(res.call) {
                val requestStartTime = attributes[requestStartTime]
                val executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - requestStartTime)

                logger.info(
                    Markers.appendEntries(
                        mapOf<String, Any?>(
                            X_UPSTREAM_HOST to request.url.host,
                            X_REQUEST_URI to request.url.encodedPath,
                            X_REQUEST_METHOD to request.method,
                            X_RESPONSE_CODE to response.status.value,
                            RESPONSE_TIME to executionTime,
                        ),
                    ),
                    "{} {} {} in {} ms",
                    request.method.value,
                    request.url,
                    response.status.value,
                    executionTime,
                )
            }

            return@intercept
        }
    }
}
