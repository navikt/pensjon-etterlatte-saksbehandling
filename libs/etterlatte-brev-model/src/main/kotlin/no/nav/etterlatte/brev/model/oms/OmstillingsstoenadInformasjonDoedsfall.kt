package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataRedigerbar

data class OmstillingsstoenadInformasjonDoedsfallData(
    val avdoedNavn: String,
    val borIutland: Boolean,
)

class OmstillingsstoenadInformasjonDoedsfall(
    override val data: OmstillingsstoenadInformasjonDoedsfallData,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            borIutland: Boolean,
            avdoedNavn: String,
        ): OmstillingsstoenadInformasjonDoedsfall =
            OmstillingsstoenadInformasjonDoedsfall(
                data = OmstillingsstoenadInformasjonDoedsfallData(
                    avdoedNavn = avdoedNavn,
                    borIutland = borIutland,
                ),
            )
    }
}
