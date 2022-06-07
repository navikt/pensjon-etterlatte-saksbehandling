package model

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GrunnbeloepTest {

    @Test
    fun `Sjekk spesifikk G gir rett beloep`(){
        val tidspunkt = LocalDate.of(2022,1,1)
        val grunnbeloep = Grunnbeloep.hentGjeldendeG(tidspunkt)
        Assertions.assertEquals(grunnbeloep.grunnbeløpPerMåned,8867 )
    }
    @Test
    fun `sjekk siste G i periode gir rett beloep`(){
        val tidspunkt = LocalDate.of(2022,1,1)
        val grunnbeloep = Grunnbeloep.hentGforPeriode(tidspunkt)
        Assertions.assertEquals(grunnbeloep.first().grunnbeløpPerMåned,8867 )
        Assertions.assertEquals(grunnbeloep.last().grunnbeløpPerMåned,9290 )
        Assertions.assertEquals(grunnbeloep.size, 2)
    }

}