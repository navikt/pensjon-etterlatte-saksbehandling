package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import java.time.LocalDate

data class BarnepensjonOpphoer(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val virkningsdato: LocalDate,
    val feilutbetaling: FeilutbetalingType,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            generellBrevData: GenerellBrevData,
            utlandstilknytning: UtlandstilknytningType?,
            brevutfall: BrevutfallDto,
        ): BarnepensjonOpphoer {
            val feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg))

            return BarnepensjonOpphoer(
                innhold = innhold.innhold(),
                innholdForhaandsvarsel =
                    vedleggHvisFeilutbetaling(
                        feilutbetaling,
                        innhold,
                        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                    ),
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                virkningsdato = requireNotNull(generellBrevData.forenkletVedtak?.virkningstidspunkt?.atDay(1)),
                feilutbetaling = feilutbetaling,
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
