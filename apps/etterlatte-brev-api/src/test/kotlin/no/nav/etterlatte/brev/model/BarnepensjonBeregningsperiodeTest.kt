package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.brev.beregningsperiode
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.pensjon.brevbaker.api.model.Kroner
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnepensjonBeregningsperiodeTest {
    @Test
    fun `mapping fra Beregningsperiode`() {
        val barnepensjonBeregningsperiode =
            BarnepensjonBeregningsperiode.fra(
                beregningsperiode(
                    datoFOM = LocalDate.of(2024, 4, 7),
                    datoTOM = LocalDate.of(2024, 9, 17),
                    grunnbeloep = Kroner(529),
                    antallBarn = 9308,
                    utbetaltBeloep = Kroner(936),
                    trygdetid = 23,
                    trygdetidForIdent = "01019898765",
                    prorataBroek = IntBroek(2, 6),
                    institusjon = true,
                    avdoedeForeldre = listOf("abc"),
                    harForeldreloessats = false,
                ),
                erForeldreloes = false,
            )
        with(barnepensjonBeregningsperiode) {
            datoFOM shouldBe LocalDate.of(2024, 4, 7)
            datoTOM shouldBe LocalDate.of(2024, 9, 17)
            grunnbeloep shouldBe Kroner(529)
            antallBarn shouldBe 9308
            utbetaltBeloep shouldBe Kroner(936)
            trygdetidForIdent shouldBe "01019898765"
            avdoedeForeldre shouldBe listOf("abc")
            harForeldreloessats shouldBe false
        }
    }

    @Test
    fun `mapping fra Beregningsdomenet til brevdomenet med defaulting til erForeldreloes`() {
        val barnepensjonBeregningsperiode =
            BarnepensjonBeregningsperiode.fra(
                beregningsperiode(
                    datoFOM = LocalDate.of(2024, 4, 7),
                    datoTOM = LocalDate.of(2024, 9, 17),
                    grunnbeloep = Kroner(529),
                    antallBarn = 9308,
                    utbetaltBeloep = Kroner(936),
                    trygdetid = 23,
                    trygdetidForIdent = null,
                    prorataBroek = IntBroek(2, 6),
                    institusjon = false,
                    avdoedeForeldre = listOf("abc"),
                    harForeldreloessats = null,
                ),
                erForeldreloes = true,
            )
        with(barnepensjonBeregningsperiode) {
            datoFOM shouldBe LocalDate.of(2024, 4, 7)
            datoTOM shouldBe LocalDate.of(2024, 9, 17)
            grunnbeloep shouldBe Kroner(529)
            antallBarn shouldBe 9308
            utbetaltBeloep shouldBe Kroner(936)
            trygdetidForIdent shouldBe null
            avdoedeForeldre shouldBe listOf("abc")
            harForeldreloessats shouldBe true
        }
    }
}
