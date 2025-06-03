package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
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
            utlandstilknytning: UtlandstilknytningType?,
            brevutfall: BrevutfallDto,
            virkningsdato: LocalDate?,
        ): BarnepensjonOpphoer {
            val feilutbetaling =
                krevIkkeNull(brevutfall.feilutbetaling?.valg?.let(::toFeilutbetalingType)) {
                    "Feilutbetaling mangler i brevutfall"
                }

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
                virkningsdato = krevIkkeNull(virkningsdato) { "Virkningsdato mangler" },
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
                feilutbetaling =
                    krevIkkeNull(brevutfall.feilutbetaling?.valg?.let(::toFeilutbetalingType)) {
                        "Feilutbetaling mangler i brevutfall"
                    },
            )
    }
}
