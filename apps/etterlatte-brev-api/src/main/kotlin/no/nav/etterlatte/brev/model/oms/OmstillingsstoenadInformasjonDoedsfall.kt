package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.model.BrevDataRedigerbar

class OmstillingsstoenadInformasjonDoedsfall(
    val avdoedNavn: String,
    val borIutland: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            borIutland: Boolean,
            avdoede: List<Avdoed>,
        ): OmstillingsstoenadInformasjonDoedsfall =
            OmstillingsstoenadInformasjonDoedsfall(
                avdoedNavn = avdoede.first().navn,
                borIutland = borIutland,
            )
    }
}
