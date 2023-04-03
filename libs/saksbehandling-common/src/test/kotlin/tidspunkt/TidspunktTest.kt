package tidspunkt

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.standardTidssoneUTC
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZonedDateTime

class TidspunktTest {
    @Test
    fun `konverterer riktig til LocalDate`() {
        val localDate = LocalDate.of(2022, Month.JUNE, 2)
        val tidspunkt =
            Tidspunkt(Instant.from(ZonedDateTime.of(localDate, LocalTime.of(23, 0, 0), standardTidssoneUTC)))
        assertEquals(localDate, tidspunkt.toLocalDate())
        assertEquals(localDate.plusDays(1), tidspunkt.toNorskLocalDate())
    }

    @Test
    fun `medTid ignorerer mindre enn sekunder`() {
        val localDate = LocalDate.of(2022, Month.JUNE, 2)
        val tidspunkt =
            Tidspunkt(Instant.from(ZonedDateTime.of(localDate, LocalTime.MIDNIGHT, standardTidssoneUTC)))
        val tid = LocalTime.of(20, 10, 5, 5)
        val medTid = tidspunkt.medTimeMinuttSekund(tid)
        assertEquals(
            medTid,
            Tidspunkt(Instant.from(ZonedDateTime.of(localDate, LocalTime.of(20, 10, 5), standardTidssoneUTC)))
        )
    }

    @Test
    fun `tidspunktet blir runda av`() {
        val instant =
            Instant.from(
                ZonedDateTime.of(
                    LocalDate.of(2022, Month.JUNE, 2),
                    LocalTime.of(10, 11, 12, 13),
                    standardTidssoneUTC
                )
            )
        val tidspunkt = Tidspunkt(instant)
        assertNotEquals(instant, tidspunkt.instant)
        assertEquals(tidspunkt.instant, instant.truncatedTo(Tidspunkt.unit))
    }
}