package no.nav.etterlatte.libs.regler.beregning

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.libs.regler.finnAlleKnekkpunkter
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class BeregnBarnepensjonTest {
    private val saksbehandler = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())
    private val grunnlag = BarnepensjonGrunnlag(
        grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = BigDecimal(100_550)),
        antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 2),
        avdoedForelder = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Avdød forelders trygdetid",
            verdi = AvdoedForelder(trygdetid = BigDecimal(30))
        )
    )

    @Test
    fun `Skal eksekvere regel for barnepensjon`() {
        val periode = RegelPeriode(LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1))
        println(beregnBarnepensjonRegel.anvend(grunnlag, periode).toJson())

        val resultat = beregnBarnepensjonRegel.eksekver(grunnlag, periode)
        println(resultat.toJson())
    }

    @Test
    fun `Skal finne alle knekkpunktene i regelverket`() {
        val knekkpunkter = beregnBarnepensjonRegel.finnAlleKnekkpunkter()

        knekkpunkter.size shouldBe 2
    }
}