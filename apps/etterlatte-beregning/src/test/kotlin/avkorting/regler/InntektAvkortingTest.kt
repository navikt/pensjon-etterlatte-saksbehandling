package no.nav.etterlatte.beregning.regler.avkorting.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlag
import no.nav.etterlatte.avkorting.regler.InntektAvkortingGrunnlagWrapper
import no.nav.etterlatte.avkorting.regler.avkortingFaktor
import no.nav.etterlatte.avkorting.regler.kroneavrundetInntektAvkorting
import no.nav.etterlatte.avkorting.regler.overstegetInntektPerMaaned
import no.nav.etterlatte.beregning.regler.inntektAvkortingGrunnlag
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class InntektAvkortingTest {
    @Test
    fun `avkortingsfaktor er 45 prosent`() {
        val faktor = avkortingFaktor.anvend(inntektAvkortingGrunnlag(), RegelPeriode(LocalDate.of(2024, 1, 1)))
        faktor.verdi shouldBe Beregningstall(0.45)
    }

    /*
    TODO nedrunding flyttes til INntektInnvilgetPeriode.kt
    @Test
    fun `inntekt rundes opp til naermeste tusen, fratrekkes opptjent utenfor periode og justeres til maanedsinntekt`() {
        val inntekt =
            maanedsinntekt.anvend(
                inntektAvkortingGrunnlag(
                    inntekt = 400999,
                    relevanteMaaneder = 10,
                ),
                RegelPeriode(LocalDate.of(2024, 1, 1)),
            )
        inntekt.verdi.toInteger() shouldBe 45000
    }
     */

    @Test // Unngår at vi deler på null (antall måneder)
    fun `oversteget inntekt per maaned er null hvis ingen innvilgede maaneder`() {
        val overstegetInntekt =
            overstegetInntektPerMaaned.anvend(
                grunnlag =
                    InntektAvkortingGrunnlagWrapper(
                        inntektAvkortingGrunnlag =
                            FaktumNode(
                                verdi =
                                    InntektAvkortingGrunnlag(
                                        inntektInnvilgetNedrundet = Beregningstall(65_000),
                                        relevanteMaaneder = Beregningstall(0),
                                        grunnlagId = UUID.randomUUID(),
                                    ),
                                kilde = "",
                                beskrivelse = "",
                            ),
                    ),
                periode = RegelPeriode(LocalDate.of(2024, 1, 1)),
            )
        overstegetInntekt.verdi.toInteger() shouldBe 0
    }

    @Test
    fun `oversteget inntekt er alt over et halvt maanedlig grunnbeloep`() {
        val overstegetInntekt =
            overstegetInntektPerMaaned.anvend(
                inntektAvkortingGrunnlag(inntekt = 120000),
                RegelPeriode(LocalDate.of(2024, 1, 1)),
            )
        overstegetInntekt.verdi.toInteger() shouldBe 5057
    }

    @Test
    fun `oversteget inntekt skal gi 0 naar inntekt er mindre en halvt grunnbeloep`() {
        val overstegetInntekt =
            overstegetInntektPerMaaned.anvend(
                inntektAvkortingGrunnlag(inntekt = 25000),
                RegelPeriode(LocalDate.of(2024, 1, 1)),
            )
        overstegetInntekt.verdi.toInteger() shouldBe 0
    }

    @Test
    fun `avkortingsbeloep er oversteget inntekt ganget med avkortingsfaktor`() {
        val avkortingsbeloep =
            kroneavrundetInntektAvkorting.anvend(
                inntektAvkortingGrunnlag(inntekt = 500000, relevanteMaaneder = 12),
                RegelPeriode(LocalDate.of(2024, 1, 1)),
            )
        avkortingsbeloep.verdi shouldBe 16526
    }
}
