package person

import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering.FORTROLIG
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering.UGRADERT
import no.nav.etterlatte.libs.common.person.hentPrioritertGradering
import org.junit.jupiter.api.Assertions.assertEquals
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
}
