package no.nav.etterlatte

import org.slf4j.LoggerFactory
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean

class ShutdownInProgressException(
    detail: String,
    cause: Throwable,
) : Exception(detail, cause)

val shuttingDown: AtomicBoolean = AtomicBoolean(false)

val logger = LoggerFactory.getLogger("shutdownhookLogger")

fun addShutdownHook(timers: Collection<Timer>) = addShutdownHook(*timers.toTypedArray())

fun addShutdownHook(vararg timer: Timer) {
    try {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                shuttingDown.set(true)
                timer.forEach { it.cancel() }
            },
        )
    } catch (e: IllegalStateException) {
        logger.warn("App er på vei ned allerede, kan ikke legge til shutdownhooks", e)
    }
}
