package no.nav.etterlatte.libs.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.util.AttributeKey
import net.logstash.logback.marker.Markers
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

internal class ClientCallLogging private constructor() {
    private val logger = LoggerFactory.getLogger(ClientCallLogging::class.java)
    private val requestStartTime = AttributeKey<Long>("requestStartTime")

    // Kun dummy pga krav
    class Config

    companion object : HttpClientPlugin<Config, ClientCallLogging> {
        override val key: AttributeKey<ClientCallLogging> = AttributeKey("ClientCallLogging")

        override fun prepare(block: Config.() -> Unit): ClientCallLogging {
            return ClientCallLogging()
        }

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
        client.receivePipeline.intercept(HttpReceivePipeline.State) { res ->
            with(res.call) {
                val requestStartTime = attributes[requestStartTime]
                val executionTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - requestStartTime)

                logger.info(
                    Markers.appendEntries(
                        mapOf<String, Any?>(
                            "x_upstream_host" to request.url.host,
                            "x_request_uri" to request.url.encodedPath,
                            "x_request_method" to request.method,
                            "response_code" to response.status.value,
                            "response_time" to executionTime,
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
