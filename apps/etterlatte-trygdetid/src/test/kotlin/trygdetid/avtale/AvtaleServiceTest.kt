package no.nav.etterlatte.trygdetid.avtale

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.Month
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AvtaleServiceTest {

    private val service = AvtaleService()

    @Test
    fun `skal levere informasjon om avtaler`() {
        val avtaler = service.hentAvtaler()

        avtaler.size shouldBe 26
        with(avtaler.first { it.kode == "EOS_NOR" }) {
            this.beskrivelse shouldBe "Eøs-Avtalen/Nordisk Konvensjon"
            this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
        }

        with(avtaler.first { it.kode == "BIH" }) {
            this.beskrivelse shouldBe "Bosnia-Hercegovina"
            this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)

            val trygdetidDato = this.datoer.first()

            trygdetidDato.kode shouldBe "BIH1992"
            trygdetidDato.beskrivelse shouldBe "01.03.1992"
            trygdetidDato.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
        }
    }

    @Test
    fun `skal levere informasjon om avtale kriterier`() {
        val avtaler = service.hentAvtaleKriterier()

        avtaler.size shouldBe 7
        with(avtaler.first { it.kode == "YRK_MEDL" }) {
            this.beskrivelse shouldBe "Yrkesaktiv i Norge eller EØS, ett års medlemskap i Norge"
            this.fraDato shouldBe LocalDate.of(1899, Month.DECEMBER, 31)
        }
    }
}