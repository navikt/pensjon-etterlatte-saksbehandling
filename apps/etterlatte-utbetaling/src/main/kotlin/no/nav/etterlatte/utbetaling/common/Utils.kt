package no.nav.etterlatte.utbetaling.common

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.xml.datatype.DatatypeConstants
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

fun Int.januar(year: Int): LocalDate = LocalDate.of(year, Month.JANUARY, this)

fun Int.februar(year: Int): LocalDate = LocalDate.of(year, Month.FEBRUARY, this)

fun Int.mars(year: Int): LocalDate = LocalDate.of(year, Month.MARCH, this)

fun Int.april(year: Int): LocalDate = LocalDate.of(year, Month.APRIL, this)

fun Int.mai(year: Int): LocalDate = LocalDate.of(year, Month.MAY, this)

fun Int.juni(year: Int): LocalDate = LocalDate.of(year, Month.JUNE, this)

fun Int.juli(year: Int): LocalDate = LocalDate.of(year, Month.JULY, this)

fun Int.august(year: Int): LocalDate = LocalDate.of(year, Month.AUGUST, this)

fun Int.september(year: Int): LocalDate = LocalDate.of(year, Month.SEPTEMBER, this)

fun Int.oktober(year: Int): LocalDate = LocalDate.of(year, Month.OCTOBER, this)

fun Int.november(year: Int): LocalDate = LocalDate.of(year, Month.NOVEMBER, this)

fun Int.desember(year: Int): LocalDate = LocalDate.of(year, Month.DECEMBER, this)

fun tidspunktMidnattIdag(clock: Clock = utcKlokke()): Tidspunkt = Tidspunkt.ofNorskTidssone(LocalDate.now(clock), LocalTime.MIDNIGHT)

fun forsteDagIMaaneden(yearMonth: YearMonth) = yearMonth.atDay(1)

fun sisteDagIMaaneden(yearMonth: YearMonth) = yearMonth.atEndOfMonth()

fun LocalDate.toXMLDate(): XMLGregorianCalendar =
    DatatypeFactory
        .newInstance()
        .newXMLGregorianCalendar(
            LocalDateTime.of(this, LocalTime.MIDNIGHT).format(tidsstempleMilliOppdrag),
        ).apply {
            timezone = DatatypeConstants.FIELD_UNDEFINED
        }

fun UUID.toUUID30() =
    this
        .toString()
        .replace("-", "")
        .substring(0, 30)
        .let { UUID30(it) }

data class UUID30(
    val value: String,
)

const val ANTALL_DETALJER_PER_AVSTEMMINGMELDING_OPPDRAG = 30

val tidsstempelDatoOppdrag = DateTimeFormatter.ofPattern("yyyy-MM-dd")
val tidsstempelTimeOppdrag = DateTimeFormatter.ofPattern("yyyyMMddHH")
val tidsstempelMikroOppdrag = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS")
val tidsstempleMilliOppdrag = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
