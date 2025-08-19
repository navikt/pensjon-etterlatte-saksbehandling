package no.nav.etterlatte.libs.common.behandling.etteroppgjoer

data class PensjonsgivendeInntekt(
    val inntektsaar: Int,
    val skatteordning: String,
    val loensinntekt: Int,
    val naeringsinntekt: Int,
    val fiskeFangstFamiliebarnehage: Int,
)
