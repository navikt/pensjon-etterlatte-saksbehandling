package model

import no.nav.etterlatte.model.VilkaarService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class VilkaarServiceTest {
    companion object {
        val service = VilkaarService()
    }
    fun assertVirkfom(doedsdato: LocalDate, mottat: LocalDate, forventetVirk: YearMonth) {
        assertEquals(forventetVirk, service.hentVirkningstidspunkt(doedsdato, mottat))
    }

    @Test
    fun `virkFomErFoersteIMaanedEtterDoedsfall`(){
        assertVirkfom(LocalDate.of(2022, 3, 5), LocalDate.of(2022, 4, 2), YearMonth.of(2022, 4))
        assertVirkfom(LocalDate.of(2021, 2, 5), LocalDate.of(2022, 4, 2), YearMonth.of(2021, 3))
        assertVirkfom(LocalDate.of(2012, 1, 5), LocalDate.of(2022, 3, 2), YearMonth.of(2019, 3))
    }
}



