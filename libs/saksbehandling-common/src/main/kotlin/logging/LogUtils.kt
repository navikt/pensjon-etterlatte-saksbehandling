package no.nav.etterlatte.libs.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*

const val CORRELATION_ID: String = "correlation_id"
const val NAV_CONSUMER_ID: String = "Nav-Consumer-Id"

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

fun sikkerLoggOppstartOgAvslutning(applikasjonsnavn: String) {
    val sikkerLogg = sikkerlogger()
    sikkerLogg.info("SikkerLogg: $applikasjonsnavn oppstart")
    Runtime.getRuntime().addShutdownHook(
        Thread {
            sikkerLogg.debug("SikkerLogg: $applikasjonsnavn avslutter")
        },
    )
}

fun sikkerlogger(): Logger = LoggerFactory.getLogger("sikkerLogg")
