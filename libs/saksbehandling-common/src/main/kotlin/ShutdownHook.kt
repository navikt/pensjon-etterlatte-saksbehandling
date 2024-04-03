package no.nav.etterlatte

import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean

val shuttingDown: AtomicBoolean = AtomicBoolean(false)

fun addShutdownHook(timers: Collection<Timer>) = addShutdownHook(*timers.toTypedArray())

fun addShutdownHook(vararg timer: Timer): Unit =
    Runtime.getRuntime().addShutdownHook(
        Thread {
            shuttingDown.set(true)
            timer.forEach { it.cancel() }
        },
    )
