package beregning.barnepensjon1967

import FaktumNode
import beregning.AvdoedForelder
import beregning.BarnepensjonGrunnlag
import beregning.beregnBarnepensjonRegel
import finnAlleKnekkpunkter
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

class Barnepensjon1967Test {
    private val saksbehandler = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())
    private val grunnlag = BarnepensjonGrunnlag(
        grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = BigDecimal(100_550)),
        antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 2),
        avdoedForelder = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Avdød forelders trygdetid",
            verdi = AvdoedForelder(trygdetid = BigDecimal(30))
        ),
        virkningstidspunkt = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Virkningstidspunkt",
            verdi = YearMonth.of(2022, 1)
        )
    )

    @Test
    fun `Regler skal representeres som et tre`() {
        println(beregnBarnepensjonRegel.anvend(grunnlag).toJson())
    }

    @Test
    fun `Skal finne alle knekkpunktene i regelverket`() {
        val knekkpunkter = beregnBarnepensjonRegel.finnAlleKnekkpunkter()

        knekkpunkter.size shouldBe 2
    }
}