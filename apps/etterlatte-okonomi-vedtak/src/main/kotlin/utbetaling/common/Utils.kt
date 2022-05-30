package no.nav.etterlatte.utbetaling.common

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

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
        .toNorskTid()
        .truncatedTo(ChronoUnit.DAYS) // 00.00 norsk tid
        .toTidspunkt()


fun forsteDagIMaaneden(yearMonth: YearMonth) =
    LocalDate.of(yearMonth.year, yearMonth.month, 1)

fun sisteDagIMaaneden(yearMonth: YearMonth) =
    forsteDagIMaaneden(yearMonth).with(TemporalAdjusters.lastDayOfMonth())


fun LocalDate.toXMLDate(): XMLGregorianCalendar {
    return DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(GregorianCalendar.from(atStartOfDay(norskTidssone))).apply {
            timezone = DatatypeConstants.FIELD_UNDEFINED
        }
}