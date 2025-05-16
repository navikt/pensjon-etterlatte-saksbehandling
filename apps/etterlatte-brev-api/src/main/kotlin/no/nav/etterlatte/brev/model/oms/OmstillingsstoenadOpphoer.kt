package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import java.time.LocalDate

data class OmstillingsstoenadOpphoer(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    val bosattUtland: Boolean,
    val virkningsdato: LocalDate,
    val feilutbetaling: FeilutbetalingType,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            brevutfall: BrevutfallDto,
            virkningsdato: LocalDate?,
            utlandstilknytningType: UtlandstilknytningType?,
        ): OmstillingsstoenadOpphoer {
            val feilutbetaling =
                krevIkkeNull(brevutfall.feilutbetaling?.valg?.let(::toFeilutbetalingType)) {
                    "Feilutbetaling mangler i brevutfall"
                }

            return OmstillingsstoenadOpphoer(
                innhold = innholdMedVedlegg.innhold(),
                innholdForhaandsvarsel =
                    vedleggHvisFeilutbetaling(
                        feilutbetaling,
                        innholdMedVedlegg,
                        BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                    ),
                bosattUtland = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
                virkningsdato =
                    krevIkkeNull(virkningsdato) {
                        "Virkningsdato mangler i brevutfall"
                    },
                feilutbetaling = feilutbetaling,
            )
        }
    }
}

data class OmstillingsstoenadOpphoerRedigerbartUtfall(
    val feilutbetaling: FeilutbetalingType,
) : BrevDataRedigerbar {
    companion object {
        fun fra(brevutfall: BrevutfallDto): OmstillingsstoenadOpphoerRedigerbartUtfall =
            OmstillingsstoenadOpphoerRedigerbartUtfall(
                feilutbetaling =
                    krevIkkeNull(brevutfall.feilutbetaling?.valg?.let(::toFeilutbetalingType)) {
                        "Feilutbetaling mangler i brevutfall"
                    },
            )
    }
}
