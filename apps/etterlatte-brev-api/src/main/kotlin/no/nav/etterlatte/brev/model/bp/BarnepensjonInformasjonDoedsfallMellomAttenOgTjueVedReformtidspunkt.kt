package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
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
            generellBrevData: GenerellBrevData,
            borIutland: Boolean,
        ): BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt =
            BarnepensjonInformasjonDoedsfallMellomAttenOgTjueVedReformtidspunkt(
                innhold = emptyList(),
                avdoedNavn =
                    generellBrevData.personerISak.avdoede
                        .first()
                        .navn,
                borIutland = borIutland,
            )
    }
}
