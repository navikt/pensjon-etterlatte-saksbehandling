package barnepensjon.model

import no.nav.etterlatte.barnepensjon.model.VilkaarService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class VilkaarServiceTest {
    companion object {
        val service = VilkaarService()
    }
    fun assertVirkfom(doedsdato: LocalDate, mottat: LocalDate, forventetVirk: YearMonth) {
        assertEquals(forventetVirk, service.hentVirkningstidspunktFoerstegangssoeknad(doedsdato, mottat))
    }

    @Test
    fun `virkFomErFoersteIMaanedEtterDoedsfall`() {
        assertVirkfom(LocalDate.of(2022, 3, 5), LocalDate.of(2022, 4, 2), YearMonth.of(2022, 4))
        assertVirkfom(LocalDate.of(2021, 2, 5), LocalDate.of(2022, 4, 2), YearMonth.of(2021, 3))
        assertVirkfom(LocalDate.of(2012, 1, 5), LocalDate.of(2022, 3, 2), YearMonth.of(2019, 3))
        assertVirkfom(LocalDate.of(2012, 1, 5), LocalDate.of(2022, 3, 2), YearMonth.of(2019, 3))
        assertVirkfom(LocalDate.of(2021, 12, 7), LocalDate.of(2022, 3, 2), YearMonth.of(2022, 1))
    }

    @Test
    fun `hentVirkningstidspunktRevurderingSoekerDoedsfall er måneden etter dødsfallet`() {
        assertEquals(
            service.hentVirkningstidspunktRevurderingSoekerDoedsfall(LocalDate.of(2022, 3, 31)),
            YearMonth.of(2022, 4)
        )
        assertEquals(
            service.hentVirkningstidspunktRevurderingSoekerDoedsfall(LocalDate.of(2020, 12, 1)),
            YearMonth.of(2021, 1)
        )
        assertEquals(
            service.hentVirkningstidspunktRevurderingSoekerDoedsfall(LocalDate.of(2018, 12, 31)),
            YearMonth.of(2019, 1)
        )
    }
}