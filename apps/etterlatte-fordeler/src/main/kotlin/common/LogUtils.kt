package no.nav.etterlatte.common

import io.ktor.request.ApplicationRequest
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.MDC
import java.util.*

const val CORRELATION_ID: String = "correlation-id"

fun <T> withLogContext(correlationId: String?, kv: Map<String, String> = emptyMap(), block: () -> T): T =
    innerLogContext(correlationId, kv, block)

fun withLogContext(correlationId: String?, kv: Map<String, String> = emptyMap(), block: () -> Unit): Unit =
    innerLogContext(correlationId, kv, block)

private fun <T> innerLogContext(correlationId: String?, kv: Map<String, String> = emptyMap(), block: () -> T): T {
    try {
        MDC.put(CORRELATION_ID, correlationId ?: generateCorrelationId())
        kv.forEach {
            MDC.put(it.key, it.value)
        }
        return block()
    } finally {
        MDC.remove(CORRELATION_ID)
        kv.forEach {
            MDC.remove(it.key)
        }
    }
}

private fun generateCorrelationId() = UUID.randomUUID().toString()

fun correlationId(): String = MDC.get(CORRELATION_ID) ?: generateCorrelationId()

fun ApplicationRequest.correlationId(): String? = this.headers[CORRELATION_ID]

fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()