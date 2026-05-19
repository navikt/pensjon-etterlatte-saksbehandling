package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataRedigerbar

data class OmstillingsstoenadInnhentingAvOpplysningerData(
    val borIUtlandet: Boolean,
)

data class OmstillingsstoenadInnhentingAvOpplysninger(
    override val data: OmstillingsstoenadInnhentingAvOpplysningerData,
) : BrevDataRedigerbar
