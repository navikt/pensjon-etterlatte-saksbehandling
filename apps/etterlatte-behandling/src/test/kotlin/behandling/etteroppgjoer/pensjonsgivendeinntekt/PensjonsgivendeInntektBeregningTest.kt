package no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.PensjonsgivendeInntektAarResponse
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.PensjonsgivendeInntektResponse
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.regler.SubsumsjonsNode
import org.junit.jupiter.api.Test

class PensjonsgivendeInntektBeregningTest {
    @Test
    fun `kan beregne ved å summere flere inntekter gitt fra Skatt`() {
        val responsFraSkatt =
            PensjonsgivendeInntektAarResponse(
                2024,
                "123",
                listOf(
                    PensjonsgivendeInntektResponse(
                        skatteordning = "FASTLAND",
                        pensjonsgivendeInntektAvLoennsinntekt = 20000,
                        pensjonsgivendeInntektAvNaeringsinntekt = 10000,
                        pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 5000,
                    ),
                    PensjonsgivendeInntektResponse(
                        skatteordning = "SVALBARD",
                        pensjonsgivendeInntektAvLoennsinntekt = 3000,
                        pensjonsgivendeInntektAvNaeringsinntekt = 4000,
                        pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage = 1500,
                    ),
                ),
            )
        val beregnet = PensjonsgivendeInntektBeregning.beregnInntekt(responsFraSkatt, 2024)
        beregnet.verdi.summertInntekt shouldBe 43500
        beregnet.verdi.loensinntekt shouldBe 23000
        beregnet.verdi.naeringsinntekt shouldBe 20500

        beregnet.toJsonNode().toString().shouldContain("20000")
        println(beregnet.toJsonNode())
        val inntekstgrunnlag =
            beregnet.noder.single { (it as SubsumsjonsNode).regel.beskrivelse == "Henter inntekter fra grunnlaget" }
        inntekstgrunnlag.verdi shouldBe responsFraSkatt
    }

    @Test
    fun `kan beregne med tom respons fra Skatt`() {
        val beregnet =
            PensjonsgivendeInntektBeregning.beregnInntekt(
                PensjonsgivendeInntektAarResponse(
                    2024,
                    "123",
                    emptyList(),
                ),
                2024,
            )
        beregnet.verdi.summertInntekt shouldBe 0
        beregnet.verdi.loensinntekt shouldBe 0
        beregnet.verdi.naeringsinntekt shouldBe 0
    }
}
