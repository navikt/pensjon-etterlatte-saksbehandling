package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class BarnepensjonOpphoer(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val feilutbetaling: FeilutbetalingType,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            utlandstilknytning: UtlandstilknytningType?,
            brevutfall: BrevutfallDto,
        ): BarnepensjonOpphoer {
            val feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg))

            return BarnepensjonOpphoer(
                innhold = innhold.innhold(),
                innholdForhaandsvarsel = vedleggHvisFeilutbetaling(feilutbetaling, innhold),
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
            )
        }
    }
}

data class BarnepensjonOpphoerRedigerbarUtfall(
    val feilutbetaling: FeilutbetalingType,
) : BrevDataRedigerbar {
    companion object {
        fun fra(brevutfall: BrevutfallDto): BarnepensjonOpphoerRedigerbarUtfall =
            BarnepensjonOpphoerRedigerbarUtfall(
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
            )
    }
}
