package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevdataMedInnhold
import no.nav.etterlatte.brev.model.Slate

class OmstillingsstoenadInformasjonDoedsfall(
    override val innhold: List<Slate.Element>,
    val avdoedNavn: String,
    val borIutland: Boolean,
) : BrevDataRedigerbar,
    BrevdataMedInnhold {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            borIutland: Boolean,
        ): OmstillingsstoenadInformasjonDoedsfall =
            OmstillingsstoenadInformasjonDoedsfall(
                innhold = emptyList(),
                avdoedNavn =
                    generellBrevData.personerISak.avdoede
                        .first()
                        .navn,
                borIutland = borIutland,
            )
    }
}
