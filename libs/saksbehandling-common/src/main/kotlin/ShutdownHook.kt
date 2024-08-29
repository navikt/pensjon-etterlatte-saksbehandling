package no.nav.etterlatte

import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean

class ShutdownInProgressException(
    detail: String,
    cause: Throwable,
) : Exception(detail, cause)

val shuttingDown: AtomicBoolean = AtomicBoolean(false)

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
        throw ShutdownInProgressException("App er på vei ned allerede, kan ikke legge til shutdownhooks", e)
    }
}
