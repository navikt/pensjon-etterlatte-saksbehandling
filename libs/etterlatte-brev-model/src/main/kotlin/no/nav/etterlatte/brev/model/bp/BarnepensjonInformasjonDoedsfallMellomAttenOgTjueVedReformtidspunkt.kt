package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataRedigerbar

data class BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunktData(
    val avdoedNavn: String,
    val borIutland: Boolean,
)

data class BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
    override val data: BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunktData,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            borIutland: Boolean,
            avdoedNavn: String,
        ): BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt =
            BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
                data = BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunktData(
                    avdoedNavn = avdoedNavn,
                    borIutland = borIutland,
                ),
            )
    }
}
