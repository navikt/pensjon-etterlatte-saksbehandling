package no.nav.etterlatte.jobs

import no.nav.etterlatte.libs.common.logging.withLogContext
import org.slf4j.Logger
import java.util.Date
import java.util.Timer
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.fixedRateTimer

data class LoggerInfo(val logger: Logger, val sikkerLogg: Logger? = null, val loggTilSikkerLogg: Boolean = false)

fun fixedRateCancellableTimer(
    name: String?,
    initialDelay: Long,
    period: Long,
    loggerInfo: LoggerInfo,
    action: (correlationId: String) -> Unit,
) = fixedRateTimer(
    name = name,
    daemon = true,
    initialDelay = initialDelay,
    period = period,
) {
    run(action, loggerInfo.logger, name, loggerInfo.sikkerLogg, loggerInfo.loggTilSikkerLogg)
}

fun fixedRateCancellableTimer(
    name: String?,
    startAt: Date,
    period: Long,
    loggerInfo: LoggerInfo,
    action: (correlationId: String) -> Unit,
) = fixedRateTimer(
    name = name,
    daemon = true,
    startAt = startAt,
    period = period,
) {
    run(action, loggerInfo.logger, name, loggerInfo.sikkerLogg, loggerInfo.loggTilSikkerLogg)
}

private fun run(
    action: (correlationID: String) -> Unit,
    logger: Logger,
    name: String?,
    sikkerLogg: Logger?,
    loggTilSikkerLogg: Boolean,
) = try {
    val correlationId = UUID.randomUUID().toString()
    withLogContext(correlationId) {
        action(correlationId)
    }
} catch (throwable: Throwable) {
    if (!shuttingDown.get()) {
        if (loggTilSikkerLogg) {
            logger.error("Jobb $name feilet, se sikker logg for stacktrace")
            sikkerLogg!!.error("Jobb $name feilet", throwable)
        } else {
            logger.error("Jobb $name feilet", throwable)
        }
    } else {
        if (loggTilSikkerLogg) {
            logger.info("Jobb $name feilet mens applikasjonen avsluttet, se sikker logg for stacktrace")
            sikkerLogg!!.info("Jobb $name feilet mens applikasjonen avsluttet", throwable)
        } else {
            logger.info("Jobb $name feilet mens applikasjonen avsluttet, se sikker logg for stacktrace", throwable)
        }
    }
}

val shuttingDown: AtomicBoolean = AtomicBoolean(false)

fun addShutdownHook(timers: Set<Timer>) = addShutdownHook(*timers.toTypedArray())

fun addShutdownHook(vararg timer: Timer): Unit =
    Runtime.getRuntime().addShutdownHook(
        Thread {
            shuttingDown.set(true)
            timer.forEach { it.cancel() }
        },
    )
