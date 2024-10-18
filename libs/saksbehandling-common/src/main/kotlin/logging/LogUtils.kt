package no.nav.etterlatte.libs.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.UUID

const val CORRELATION_ID: String = "x_correlation_id"

fun <T> withLogContext(
    correlationId: String? = null,
    kv: Map<String, String> = emptyMap(),
    block: () -> T,
): T = innerLogContext(correlationId, kv, block)

fun withLogContext(
    correlationId: String? = null,
    kv: Map<String, String> = emptyMap(),
    block: () -> Unit,
): Unit = innerLogContext(correlationId, kv, block)

private fun <T> innerLogContext(
    correlationId: String?,
    kv: Map<String, String> = emptyMap(),
    block: () -> T,
): T {
    var exceptionThrown = false

    try {
        MDC.put(CORRELATION_ID, correlationId ?: generateCorrelationId())
        kv.forEach {
            MDC.put(it.key, it.value)
        }
        return block()
    } catch (e: Exception) {
        exceptionThrown = true

        throw e
    } finally {
        if (!exceptionThrown) {
            MDC.remove(CORRELATION_ID)
            kv.forEach {
                MDC.remove(it.key)
            }
        }
    }
}

private fun generateCorrelationId() = UUID.randomUUID().toString()

fun getCorrelationId(): String = MDC.get(CORRELATION_ID) ?: generateCorrelationId()

fun sikkerLoggOppstart(applikasjonsnavn: String) {
    val sikkerLogg = sikkerlogger()
    sikkerLogg.info("SikkerLogg: $applikasjonsnavn oppstart")
}

fun sikkerlogger(): Logger = LoggerFactory.getLogger("sikkerLogg")
