package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class BarnepensjonRevurdering(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    val erEndret: Boolean,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val harFlereUtbetalingsperioder: Boolean,
    val feilutbetaling: FeilutbetalingType,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            utbetalingsinfo: Utbetalingsinfo,
            forrigeUtbetalingsinfo: Utbetalingsinfo?,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            grunnbeloep: Grunnbeloep,
            utlandstilknytning: UtlandstilknytningType?,
            brevutfall: BrevutfallDto,
        ): BarnepensjonRevurdering {
            val beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo)
            val feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg))

            return BarnepensjonRevurdering(
                innhold = innhold.innhold(),
                innholdForhaandsvarsel =
                    vedleggHvisFeilutbetaling(
                        feilutbetaling,
                        innhold,
                        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                    ),
                erEndret = forrigeUtbetalingsinfo == null || forrigeUtbetalingsinfo.beloep != utbetalingsinfo.beloep,
                beregning = barnepensjonBeregning(innhold, utbetalingsinfo, grunnbeloep, beregningsperioder, trygdetid),
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraBarnepensjonBeregningsperioder(dto, beregningsperioder) },
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                harFlereUtbetalingsperioder = utbetalingsinfo.beregningsperioder.size > 1,
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(BarnepensjonInnvilgelse.tidspunktNyttRegelverk) ||
                            it.datoFOM.isEqual(
                                BarnepensjonInnvilgelse.tidspunktNyttRegelverk,
                            )
                    },
                feilutbetaling = feilutbetaling,
            )
        }
    }
}

data class BarnepensjonRevurderingRedigerbartUtfall(
    val erEtterbetaling: Boolean,
    val harUtbetaling: Boolean,
    val feilutbetaling: FeilutbetalingType,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            etterbetaling: EtterbetalingDTO?,
            utbetalingsinfo: Utbetalingsinfo,
            brevutfall: BrevutfallDto,
        ): BarnepensjonRevurderingRedigerbartUtfall {
            return BarnepensjonRevurderingRedigerbartUtfall(
                erEtterbetaling = etterbetaling != null,
                harUtbetaling = utbetalingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
            )
        }
    }
}
