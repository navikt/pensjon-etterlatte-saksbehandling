package no.nav.etterlatte.utbetaling.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.Clock
import java.time.Instant
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId

internal class UtilsTest {

    @Test
    fun `skal returnere Tidspunkt-objekt for midnatt i dag for norsk vintertid`() {
        val statiskKlokke = Clock.fixed(Instant.parse("2022-01-01T21:14:29.4839104Z"), ZoneId.of("Europe/Oslo"))
        val midnatt = tidspunktMidnattIdag(statiskKlokke)

        assertEquals("2021-12-31T23:00:00Z", midnatt.instant.toString())
    }

    @Test
    fun `skal returnere Tidspunkt-objekt for midnatt i dag for norsk sommertid`() {
        val statiskKlokke = Clock.fixed(Instant.parse("2022-06-01T21:14:29.4839104Z"), ZoneId.of("Europe/Oslo"))
        val midnatt = tidspunktMidnattIdag(statiskKlokke)

        assertEquals("2022-05-31T22:00:00Z", midnatt.instant.toString())
    }

    @Test
    fun `skal returnere foerste dag i maaneden`() {
        val jan2022 = YearMonth.of(2022, Month.JANUARY)
        val foersteJanuar2022 = forsteDagIMaaneden(jan2022)

        assertAll("skal finne foerste dag i maaneden",
            { assertEquals(jan2022.year, foersteJanuar2022.year) },
            { assertEquals(jan2022.month, foersteJanuar2022.month) },
            { assertEquals(1, foersteJanuar2022.dayOfMonth) }
        )
    }

    @Test
    fun `skal returnere siste dag i maaneden`() {
        val jan2022 = YearMonth.of(2022, Month.JANUARY)
        val sisteDagJanuar = sisteDagIMaaneden(jan2022)

        val feb2022 = YearMonth.of(2022, Month.FEBRUARY)
        val sisteDagFebruar = sisteDagIMaaneden(feb2022)

        val april2022 = YearMonth.of(2022, Month.APRIL)
        val sisteDagApril = sisteDagIMaaneden(april2022)

        assertAll("Skal returnere siste dag i maaneden for januar, februar og april",
            { assertEquals(jan2022.year, sisteDagJanuar.year) },
            { assertEquals(jan2022.month, sisteDagJanuar.month) },
            { assertEquals(31, sisteDagJanuar.dayOfMonth) },
            { assertEquals(feb2022.year, sisteDagFebruar.year) },
            { assertEquals(feb2022.month, sisteDagFebruar.month) },
            { assertEquals(28, sisteDagFebruar.dayOfMonth) },
            { assertEquals(april2022.year, sisteDagApril.year) },
            { assertEquals(april2022.month, sisteDagApril.month) },
            { assertEquals(30, sisteDagApril.dayOfMonth) }
        )
    }
}