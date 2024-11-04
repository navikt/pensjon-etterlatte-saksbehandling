package no.nav.etterlatte.jobs

import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Date

fun Tidspunkt.next(atTime: LocalTime): Date =
    if (this.toLocalTime().isAfter(atTime)) {
        this.plus(1, ChronoUnit.DAYS).medTimeMinuttSekund(atTime).toJavaUtilDate()
    } else {
        this.medTimeMinuttSekund(atTime).toJavaUtilDate()
    }
