package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class BarnepensjonAvslag(
    override val innhold: List<Slate.Element>,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            brukerUnder18Aar: Boolean,
            utlandstilknytning: UtlandstilknytningType?,
        ): BarnepensjonAvslag =
            BarnepensjonAvslag(
                innhold = innhold.innhold(),
                brukerUnder18Aar = brukerUnder18Aar,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
            )
    }
}

data class BarnepensjonAvslagRedigerbar(
    val erSluttbehandling: Boolean,
) : BrevDataRedigerbar
