package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.BarnepensjonBeregning

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
