package no.nav.etterlatte.utbetaling.common

import no.nav.etterlatte.libs.common.tidspunkt.fixedNorskTid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.Instant
import java.time.Month
import java.time.YearMonth
import java.util.*

internal class UtilsTest {

    @Test
    fun `skal returnere Tidspunkt-objekt for midnatt i dag for norsk vintertid`() {
        val statiskKlokke = Instant.parse("2022-01-01T21:14:29.4839104Z").fixedNorskTid()
        val midnatt = tidspunktMidnattIdag(statiskKlokke)

        assertEquals("2021-12-31T23:00:00Z", midnatt.toString())
    }

    @Test
    fun `skal returnere Tidspunkt-objekt for midnatt i dag for norsk sommertid`() {
        val statiskKlokke = Instant.parse("2022-06-01T21:14:29.4839104Z").fixedNorskTid()
        val midnatt = tidspunktMidnattIdag(statiskKlokke)

        assertEquals("2022-05-31T22:00:00Z", midnatt.toString())
    }

    @Test
    fun `skal returnere foerste dag i maaneden`() {
        val jan2022 = YearMonth.of(2022, Month.JANUARY)
        val foersteJanuar2022 = forsteDagIMaaneden(jan2022)

        assertAll(
            "skal finne foerste dag i maaneden",
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

        assertAll(
            "Skal returnere siste dag i maaneden for januar, februar og april",
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

    @Test
    fun `uuid30 skal ikke inneholde bindestreker`() {
        val uuid30 = UUID.randomUUID().toUUID30()
        assertFalse(uuid30.toString().contains("-"))
    }

    @Test
    fun `uuid30 skal besta av uuids foerste 30 tegn med unntak av bindestrekene`() {
        val uuid = UUID.randomUUID()
        val uuidUtenBindestrek = uuid.toString().replace("-", "")
        val uuidUFoerste30Tegn = uuidUtenBindestrek.substring(0, 30)
        val uuid30 = uuid.toUUID30()

        assertAll(
            "uuid30 skal besta av uuids foerste 30 tegn med unntak av bindestrekene",
            { assertEquals(30, uuid30.value.length) },
            { assertEquals(uuidUFoerste30Tegn, uuid30.value) }
        )
    }
}