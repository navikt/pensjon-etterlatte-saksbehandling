package no.nav.etterlatte.model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.YearMonth

class GrunnbeloepTest {

    @Test
    fun `Sjekk spesifikk G gir rett beloep`() {
        val tidspunkt = YearMonth.of(2022, 1)
        val grunnbeloep = Grunnbeloep.hentGjeldendeG(tidspunkt)
        Assertions.assertEquals(grunnbeloep.grunnbeløpPerMåned, 8867)
    }

    @Test
    fun `sjekk siste G i periode gir rett beloep`() {
        val tidspunkt = YearMonth.of(2022, 1)
        val tidspunktSlutt = YearMonth.of(2022, 12)
        val grunnbeloep = Grunnbeloep.hentGforPeriode(tidspunkt, tidspunktSlutt)
        Assertions.assertEquals(grunnbeloep.first().grunnbeløpPerMåned, 8867)
        Assertions.assertEquals(grunnbeloep.last().grunnbeløpPerMåned, 9290)
        Assertions.assertEquals(grunnbeloep.size, 2)
    }
}