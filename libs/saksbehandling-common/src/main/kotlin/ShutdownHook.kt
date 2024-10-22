package no.nav.etterlatte

import org.slf4j.LoggerFactory
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean

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
        try {
            logger.warn("App er på vei ned allerede, kan ikke legge til shutdownhooks", e)
        } catch (e: Exception) {
            // Ignorer at vi  ikke får logget på grunn av shutdown, da er det ikke noe å gjøre uansett
        }
    }
}
