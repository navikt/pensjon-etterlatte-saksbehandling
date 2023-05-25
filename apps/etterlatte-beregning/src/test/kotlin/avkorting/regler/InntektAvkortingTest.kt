package no.nav.etterlatte.beregning.regler.avkorting.regler

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.regler.avkortingFaktor
import no.nav.etterlatte.avkorting.regler.kroneavrundetInntektAvkorting
import no.nav.etterlatte.avkorting.regler.nedrundetInntekt
import no.nav.etterlatte.avkorting.regler.overstegetInntekt
import no.nav.etterlatte.beregning.regler.inntektAvkortingGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InntektAvkortingTest {

    @Test
    fun `avkortingsfaktor er 45 prosent`() {
        val faktor = avkortingFaktor.anvend(inntektAvkortingGrunnlag(), RegelPeriode(LocalDate.now()))
        faktor.verdi shouldBe Beregningstall(0.45)
    }

    @Test
    fun `inntekt hentes fra grunnlag og rundes opp til naermeste tusen`() {
        val inntekt = nedrundetInntekt.anvend(inntektAvkortingGrunnlag(inntekt = 500999), RegelPeriode(LocalDate.now()))
        inntekt.verdi.toInteger() shouldBe 500000
    }

    @Test
    fun `oversteget inntekt er alt over et halvt grunnbeloep`() {
        val overstegetInntekt = overstegetInntekt.anvend(
            inntektAvkortingGrunnlag(inntekt = 100000),
            RegelPeriode(LocalDate.of(2023, 1, 1))
        )
        overstegetInntekt.verdi.toInteger() shouldBe 44261
    }

    @Test
    fun `oversteget inntekt skal gi 0 naar inntekt er mindre en halvt grunnbeloep`() {
        val overstegetInntekt = overstegetInntekt.anvend(
            inntektAvkortingGrunnlag(inntekt = 25000),
            RegelPeriode(LocalDate.of(2023, 1, 1))
        )
        overstegetInntekt.verdi.toInteger() shouldBe 0
    }

    @Test
    fun `avkortingsbeloep er oversteget inntekt ganget med avkortingsfaktor oppdelt i antall maaneder (12)`() {
        val avkortingsbeloep = kroneavrundetInntektAvkorting.anvend(
            inntektAvkortingGrunnlag(inntekt = 500000),
            RegelPeriode(LocalDate.of(2023, 1, 1))
        )
        avkortingsbeloep.verdi shouldBe 16660
    }
}