package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevDataRedigerbar

class OmstillingsstoenadInntektsjustering(
    val borIutland: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(borIutland: Boolean): OmstillingsstoenadInntektsjustering =
            OmstillingsstoenadInntektsjustering(
                borIutland = borIutland,
            )
    }
}
