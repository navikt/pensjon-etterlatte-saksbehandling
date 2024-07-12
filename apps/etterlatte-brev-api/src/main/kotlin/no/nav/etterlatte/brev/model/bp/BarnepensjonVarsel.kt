package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.Slate

data class BarnepensjonVarsel(
    override val innhold: List<Slate.Element>,
    val beregning: BarnepensjonBeregning?,
    val erUnder18Aar: Boolean,
    val erBosattUtlandet: Boolean,
) : BrevDataFerdigstilling

data class BarnepensjonVarselRedigerbartUtfall(
    val automatiskBehandla: Boolean,
    val erBosattUtlandet: Boolean,
) : BrevDataRedigerbar
