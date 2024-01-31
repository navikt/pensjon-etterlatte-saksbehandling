package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class BarnepensjonAvslag(
    val innhold: List<Slate.Element>,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
) : BrevData() {
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
