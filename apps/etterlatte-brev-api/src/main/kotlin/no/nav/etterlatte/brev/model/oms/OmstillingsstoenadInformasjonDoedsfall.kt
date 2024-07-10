package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avdoed
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
            borIutland: Boolean,
            avdoede: List<Avdoed>,
        ): OmstillingsstoenadInformasjonDoedsfall =
            OmstillingsstoenadInformasjonDoedsfall(
                innhold = emptyList(),
                avdoedNavn = avdoede.first().navn,
                borIutland = borIutland,
            )
    }
}
