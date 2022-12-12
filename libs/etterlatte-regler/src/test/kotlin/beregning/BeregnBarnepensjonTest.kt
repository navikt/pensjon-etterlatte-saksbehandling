package beregning

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test
import regler.FaktumNode
import regler.RegelPeriode
import regler.finnAlleKnekkpunkter
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class BeregnBarnepensjonTest {
    private val saksbehandler = Grunnlagsopplysning.Saksbehandler("Z12345", Instant.now())
    private val grunnlag = BarnepensjonGrunnlag(
        grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = BigDecimal(100_550)),
        antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 2),
        avdoedForelder = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Avdød forelders trygdetid",
            verdi = AvdoedForelder(trygdetid = BigDecimal(30))
        ),
        periode = FaktumNode(
            kilde = saksbehandler,
            beskrivelse = "Virkningstidspunkt",
            verdi = RegelPeriode(LocalDate.of(2022, 1, 1))
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