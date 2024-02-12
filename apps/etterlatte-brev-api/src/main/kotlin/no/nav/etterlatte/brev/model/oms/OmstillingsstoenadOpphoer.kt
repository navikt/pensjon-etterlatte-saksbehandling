package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import java.time.LocalDate

data class OmstillingsstoenadOpphoer(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    val bosattUtland: Boolean,
    val feilutbetaling: FeilutbetalingType,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            utlandstilknytning: Utlandstilknytning?,
            brevutfall: BrevutfallDto,
        ): OmstillingsstoenadOpphoer {
            val feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg))

            return OmstillingsstoenadOpphoer(
                innhold = innholdMedVedlegg.innhold(),
                innholdForhaandsvarsel = vedleggHvisFeilutbetaling(feilutbetaling, innholdMedVedlegg),
                bosattUtland = utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                feilutbetaling = feilutbetaling,
            )
        }
    }
}

data class OmstillingsstoenadOpphoerRedigerbartUtfall(
    val feilutbetaling: FeilutbetalingType,
    val virkningsdato: LocalDate,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            brevutfall: BrevutfallDto,
        ): OmstillingsstoenadOpphoerRedigerbartUtfall =
            OmstillingsstoenadOpphoerRedigerbartUtfall(
                virkningsdato = requireNotNull(generellBrevData.forenkletVedtak?.virkningstidspunkt?.atDay(1)),
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
            )
    }
}
