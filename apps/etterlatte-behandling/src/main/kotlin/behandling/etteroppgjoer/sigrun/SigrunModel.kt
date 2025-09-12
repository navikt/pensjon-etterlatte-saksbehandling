package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.PensjonsgivendeInntekt

data class HendelseslisteFraSkatt(
    val hendelser: List<SkatteoppgjoerHendelse>,
) {
    companion object {
        fun stub(
            startSekvensnummer: Long = 0,
            antall: Int = 10,
            aar: Int = 2024,
        ): HendelseslisteFraSkatt {
            val hendelser =
                List(antall) { index ->
                    SkatteoppgjoerHendelse(
                        gjelderPeriode = aar.toString(),
                        hendelsetype = SigrunKlient.HENDELSETYPE_NY,
                        identifikator = index.toString(),
                        sekvensnummer = startSekvensnummer + index,
                    )
                }
            return HendelseslisteFraSkatt(hendelser)
        }
    }
}

data class HendelserSekvensnummerFraSkatt(
    val sekvensnummer: Long,
)

data class SkatteoppgjoerHendelse(
    val gjelderPeriode: String?, // inntektsaar
    val hendelsetype: String?,
    val identifikator: String,
    val sekvensnummer: Long,
)

data class PensjonsgivendeInntektFraSkattSummert(
    val inntektsaar: Int,
    val loensinntekt: Int,
    val naeringsinntekt: Int,
    val fiskeFangstFamiliebarnehage: Int,
)

data class PensjonsgivendeInntektFraSkatt(
    val inntektsaar: Int,
    val inntekter: List<PensjonsgivendeInntekt>,
) {
    init {
        require(inntekter.all { it.inntektsaar == inntektsaar }) {
            "Alle inntekter må ha inntektsår = $inntektsaar, men fant: ${inntekter.map { it.inntektsaar }}"
        }
    }

    companion object {
        fun stub(
            aar: Int = 2024,
            aarsinntekt: Int = 300000,
        ) = PensjonsgivendeInntektFraSkatt(
            inntektsaar = aar,
            inntekter =
                listOf(
                    PensjonsgivendeInntekt(
                        skatteordning = "FASTLAND",
                        loensinntekt = aarsinntekt,
                        naeringsinntekt = 0,
                        fiskeFangstFamiliebarnehage = 0,
                        inntektsaar = aar,
                    ),
                    PensjonsgivendeInntekt(
                        skatteordning = "SVALBARD",
                        loensinntekt = aarsinntekt,
                        naeringsinntekt = 0,
                        fiskeFangstFamiliebarnehage = 0,
                        inntektsaar = aar,
                    ),
                ),
        )
    }
}
