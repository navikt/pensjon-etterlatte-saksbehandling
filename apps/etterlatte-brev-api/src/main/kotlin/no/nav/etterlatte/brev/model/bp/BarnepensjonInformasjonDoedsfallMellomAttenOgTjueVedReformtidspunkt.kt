package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevdataMedInnhold
import no.nav.etterlatte.brev.model.Slate

data class BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
    override val innhold: List<Slate.Element>,
    val avdoedNavn: String,
    val borIutland: Boolean,
) : BrevDataRedigerbar,
    BrevdataMedInnhold {
    companion object {
        fun fra(
            borIutland: Boolean,
            avdoede: List<Avdoed>,
        ): BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt =
            BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
                innhold = emptyList(),
                avdoedNavn = avdoede.first().navn,
                borIutland = borIutland,
            )
    }
}
