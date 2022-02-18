package no.nav.etterlatte.libs.common.logging

import org.slf4j.MDC
import java.util.*

const val CORRELATION_ID: String = "correlation-id"

suspend fun <T> withLogContextCo(correlationId: String?, kv: Map<String, String> = emptyMap(), block: suspend () -> T): T =
    coInnerLogContext(correlationId, kv, block)

fun <T> withLogContext(correlationId: String?, kv: Map<String, String> = emptyMap(), block: () -> T): T =
    innerLogContext(correlationId, kv, block)

fun withLogContext(correlationId: String?, kv: Map<String, String> = emptyMap(), block: () -> Unit): Unit =
    innerLogContext(correlationId, kv, block)

private suspend fun <T> coInnerLogContext(correlationId: String?, kv: Map<String, String> = emptyMap(), block: suspend () -> T): T {
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