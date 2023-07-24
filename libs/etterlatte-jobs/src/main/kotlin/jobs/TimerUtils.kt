package no.nav.etterlatte.jobs

import org.slf4j.Logger
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.fixedRateTimer

fun fixedRateCancellableTimer(
    name: String?,
    initialDelay: Long,
    period: Long,
    logger: Logger,
    sikkerLogg: Logger,
    action: TimerTask.() -> Unit
) = fixedRateTimer(
    name = name,
    daemon = true,
    initialDelay = initialDelay,
    period = period
) {
    run(action, logger, name, sikkerLogg)
}

fun fixedRateCancellableTimer(
    name: String?,
    startAt: Date,
    period: Long,
    logger: Logger,
    sikkerLogg: Logger,
    action: TimerTask.() -> Unit
) = fixedRateTimer(
    name = name,
    daemon = true,
    startAt = startAt,
    period = period
) {
    run(action, logger, name, sikkerLogg)
}

private fun TimerTask.run(action: TimerTask.() -> Unit, logger: Logger, name: String?, sikkerLogg: Logger) = try {
    action()
} catch (throwable: Throwable) {
    if (!shuttingDown.get()) {
        logger.error("Jobb $name feilet, se sikker logg for stacktrace")
        sikkerLogg.error("Jobb $name feilet", throwable)
    } else {
        logger.info("Jobb $name feilet mens applikasjonen avsluttet, se sikker logg for stacktrace")
        sikkerLogg.info("Jobb $name feilet mens applikasjonen avsluttet", throwable)
    }
}

val shuttingDown: AtomicBoolean = AtomicBoolean(false)

fun addShutdownHook(timers: Set<Timer>) = addShutdownHook(*timers.toTypedArray())
fun addShutdownHook(vararg timer: Timer) = Runtime.getRuntime().addShutdownHook(
    Thread {
        shuttingDown.set(true)
        timer.forEach { it.cancel() }
    }
)