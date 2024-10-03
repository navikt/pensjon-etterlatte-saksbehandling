package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.behandling.Avdoed

data class BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
    val avdoedNavn: String,
    val borIutland: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            borIutland: Boolean,
            avdoede: List<Avdoed>,
        ): BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt =
            BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
                avdoedNavn = avdoede.first().navn,
                borIutland = borIutland,
            )
    }
}
