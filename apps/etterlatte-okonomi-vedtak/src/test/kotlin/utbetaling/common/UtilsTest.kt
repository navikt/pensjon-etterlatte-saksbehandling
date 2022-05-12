package no.nav.etterlatte.utbetaling.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
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
}