package no.nav.etterlatte.utbetaling.common

import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

fun ZonedDateTime.next(atTime: LocalTime): Date {
    return if (this.toLocalTime().isAfter(atTime)) {
        Date.from(
            this.plusDays(1)
                .withHour(atTime.hour)
                .withMinute(atTime.minute)
                .withSecond(atTime.second)
                .toInstant(),
        )
    } else {
        Date.from(
            this.withHour(atTime.hour)
                .withMinute(atTime.minute)
                .withSecond(atTime.second)
                .toInstant(),
        )
    }
}

fun tidspunktMidnattIdag(clock: Clock = Clock.systemUTC()): Tidspunkt =
    Tidspunkt.now(clock)
        .toZonedNorskTid()
        .truncatedTo(ChronoUnit.DAYS) // 00.00 norsk tid
        .toInstant().let {
            Tidspunkt(it)
        }
