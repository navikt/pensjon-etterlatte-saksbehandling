package no.nav.etterlatte.utbetaling.common

import java.time.LocalTime
import java.time.ZonedDateTime
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