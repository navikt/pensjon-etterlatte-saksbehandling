package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataRedigerbar

data class BarnepensjonInnhentingAvOpplysningerData(
    val erOver18aar: Boolean,
    val borIUtlandet: Boolean,
)

data class BarnepensjonInnhentingAvOpplysninger(
    override val data: BarnepensjonInnhentingAvOpplysningerData,
) : BrevDataRedigerbar
