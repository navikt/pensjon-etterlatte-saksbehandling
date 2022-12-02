package no.nav.etterlatte.utbetaling.common

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
                .toInstant()
        )
    } else {
        Date.from(
            this.withHour(atTime.hour)
                .withMinute(atTime.minute)
                .withSecond(atTime.second)
                .toInstant()
        )
    }
}

fun tidspunktMidnattIdag(clock: Clock = Clock.systemUTC()): Tidspunkt =
    Tidspunkt.now(clock)
        .toNorskTid()
        .truncatedTo(ChronoUnit.DAYS) // 00.00 norsk tid
        .toTidspunkt()

fun forsteDagIMaaneden(yearMonth: YearMonth) = yearMonth.atDay(1)

fun sisteDagIMaaneden(yearMonth: YearMonth) = yearMonth.atEndOfMonth()

fun LocalDate.toXMLDate(): XMLGregorianCalendar {
    return DatatypeFactory.newInstance()
        .newXMLGregorianCalendar(GregorianCalendar.from(atStartOfDay(norskTidssone))).apply {
            timezone = DatatypeConstants.FIELD_UNDEFINED
        }
}

fun UUID.toUUID30() = this.toString().replace("-", "").substring(0, 30).let { UUID30(it) }

data class UUID30(val value: String)

const val ANTALL_DETALJER_PER_AVSTEMMINGMELDING_OPPDRAG = 70
val tidsstempelTimeOppdrag = DateTimeFormatter.ofPattern("yyyyMMddHH")
val tidsstempelMikroOppdrag = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
val tidsstempelDatoOppdrag = DateTimeFormatter.ofPattern("yyyy-MM-dd")