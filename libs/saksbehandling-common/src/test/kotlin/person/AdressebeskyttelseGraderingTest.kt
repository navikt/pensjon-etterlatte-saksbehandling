package person

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering.FORTROLIG
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering.UGRADERT
import no.nav.etterlatte.libs.common.person.finnHoyestGradering
import no.nav.etterlatte.libs.common.person.finnHoyesteGradering
import no.nav.etterlatte.libs.common.person.hentPrioritertGradering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AdressebeskyttelseGraderingTest {
    @Test
    fun `Sjekk enum ordinal value - sikrer prioritering`() {
        assertEquals(3, UGRADERT.ordinal)
        assertEquals(2, FORTROLIG.ordinal)
        assertEquals(1, STRENGT_FORTROLIG.ordinal)
        assertEquals(0, STRENGT_FORTROLIG_UTLAND.ordinal)
    }

    @Test
    fun `Prioritering blir korrekt - UGRADERT`() {
        val ugradert = listOf(null, UGRADERT, null, null)

        assertEquals(UGRADERT, ugradert.hentPrioritertGradering())
    }

    @Test
    fun `Prioritering blir korrekt - FORTROLIG`() {
        val fortrolig = listOf(null, UGRADERT, null, FORTROLIG)

        assertEquals(FORTROLIG, fortrolig.hentPrioritertGradering())
    }

    @Test
    fun `Prioritering blir korrekt - STRENGT_FORTROLIG`() {
        val strengtFortrolig = listOf(FORTROLIG, UGRADERT, null, STRENGT_FORTROLIG, FORTROLIG)

        assertEquals(STRENGT_FORTROLIG, strengtFortrolig.hentPrioritertGradering())
    }

    @Test
    fun `Prioritering blir korrekt - STRENGT_FORTROLIG_UTLAND`() {
        val strengtFortrolig = listOf(STRENGT_FORTROLIG, UGRADERT, null, STRENGT_FORTROLIG_UTLAND, FORTROLIG)

        assertEquals(STRENGT_FORTROLIG_UTLAND, strengtFortrolig.hentPrioritertGradering())
    }

    @Test
    fun `Skal finne høyeste gradering i en liste av alle mulige`() {
        val gradering = finnHoyestGradering(AdressebeskyttelseGradering.entries)
        assertTrue(gradering in listOf(STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND))
    }

    @Test
    fun `Skal finne høyeste gradering der FORTYRLOIG er høyest`() {
        val gradering = finnHoyestGradering(listOf(FORTROLIG, UGRADERT))
        assertEquals(gradering, FORTROLIG)
    }

    @Test
    fun `Skal gi ugradert hvis ugradert er eneste`() {
        val gradering = finnHoyestGradering(listOf(UGRADERT))
        assertEquals(gradering, UGRADERT)
    }

    @Test
    fun `Skal gi ugradert hvis ingen graderinger er meldt`() {
        val gradering = finnHoyestGradering(emptyList())

        assertEquals(gradering, UGRADERT)
    }

    @Test
    fun `Skal finne høyeste gradering av to ulike - STRENGT_FORTROLIG`() {
        val gradering = finnHoyesteGradering(STRENGT_FORTROLIG, FORTROLIG)

        assertEquals(gradering, STRENGT_FORTROLIG)
    }

    @Test
    fun `Skal finne høyeste gradering av to ulike - STRENGT_FORTROLIG_UTLAND`() {
        val gradering = finnHoyesteGradering(FORTROLIG, STRENGT_FORTROLIG_UTLAND)

        assertEquals(gradering, STRENGT_FORTROLIG_UTLAND)
    }

    @Test
    fun `Skal finne høyeste gradering av to ulike - FORTROLIG`() {
        val gradering = finnHoyesteGradering(FORTROLIG, UGRADERT)

        assertEquals(gradering, FORTROLIG)
    }
}
