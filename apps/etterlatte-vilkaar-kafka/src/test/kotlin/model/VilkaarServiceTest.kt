package model

import no.nav.etterlatte.model.VilkaarService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VilkaarServiceTest {
    companion object {
        val service = VilkaarService()
    }
    fun assertVirkfom(doedsdato: LocalDate, mottat: LocalDate, forventetVirk: LocalDate) {
        assertEquals(forventetVirk, service.hentVirkningstidspunkt(doedsdato, mottat))
    }

    @Test
    fun `virkFomErFoersteIMaanedEtterDoedsfall`(){
        assertVirkfom(LocalDate.of(2022, 3, 5), LocalDate.of(2022, 4, 2), LocalDate.of(2022, 4, 1))
        assertVirkfom(LocalDate.of(2021, 2, 5), LocalDate.of(2022, 4, 2), LocalDate.of(2021, 3, 1))
        assertVirkfom(LocalDate.of(2012, 3, 5), LocalDate.of(2022, 4, 2), LocalDate.of(2019, 4, 1))
    }
}



