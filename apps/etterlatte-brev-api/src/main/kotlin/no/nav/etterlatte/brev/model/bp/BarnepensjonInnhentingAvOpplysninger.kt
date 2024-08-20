package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.model.BrevDataRedigerbar

data class BarnepensjonInnhentingAvOpplysninger(
    val erOver18aar: Boolean,
    val borIUtlandet: Boolean,
) : BrevDataRedigerbar
