package no.nav.etterlatte.behandling.etteroppgjoer.sigrun

class SigrunService {
    fun hentPensjonsgivendeInntekt(ident: String): PensjonsgivendeInntektFraSkatt {
        // TODO klient mot Sigrun
        return PensjonsgivendeInntektFraSkatt.stub()
    }
}

data class PensjonsgivendeInntektFraSkatt(
    val inntektsaar: String,
    val inntekter: List<PensjonsgivendeInntekt>,
) {
    companion object {
        fun stub(
            aar: Int = 2024,
            aarsinntekt: Int = 300000,
        ) = PensjonsgivendeInntektFraSkatt(
            inntektsaar = "2024",
            inntekter =
                listOf(
                    PensjonsgivendeInntekt(
                        skatteordning = "FASTLAND",
                        loensinntekt = aarsinntekt,
                        naeringsinntekt = 0,
                        annet = 0,
                    ),
                ),
        )
    }
}

data class PensjonsgivendeInntekt(
    val skatteordning: String,
    val loensinntekt: Int,
    val naeringsinntekt: Int,
    val annet: Int,
)

/*
TODO for klient
data class PensjonsgivendeInntektAar(
    val inntektsaar: String,
    val pensjonsgivendeInntekt: List<PensjonsgivendeInntekt>
)

data class PensjonsgivendeInntekt(
    val skatteordning: String,
    val pensjonsgivendeInntektAvLoennsinntekt: String,
    val pensjonsgivendeInntektAvNaeringsinntekt: String,
    val pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage: String,
)
*/
