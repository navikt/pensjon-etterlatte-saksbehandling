package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.model.BrevDataRedigerbar

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
