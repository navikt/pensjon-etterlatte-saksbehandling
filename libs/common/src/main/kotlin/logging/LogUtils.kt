package no.nav.etterlatte.libs.common.logging

import org.slf4j.MDC
import java.util.*

const val X_CORRELATION_ID: String = "X-Correlation-Id"
const val CORRELATION_ID: String = "correlation_id"

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

fun getCorrelationId(): String = MDC.get(CORRELATION_ID) ?: generateCorrelationId()