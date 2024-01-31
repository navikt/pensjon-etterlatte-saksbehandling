package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BarnepensjonEtterbetaling
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.EndringBrevData
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class BarnepensjonRevurdering(
    val innhold: List<Slate.Element>,
    val erEndret: Boolean,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: BarnepensjonEtterbetaling?,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val harFlereUtbetalingsperioder: Boolean,
) : EndringBrevData() {
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
        ): BrevData {
            val beregningsperioder = barnepensjonBeregningsperioder(utbetalingsinfo)

            return BarnepensjonRevurdering(
                innhold = innhold.innhold(),
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
            )
        }
    }
}

data class BarnepensjonRevurderingRedigerbartUtfall(
    val erEtterbetaling: Boolean,
) : EndringBrevData() {
    companion object {
        fun fra(etterbetaling: EtterbetalingDTO?): BrevData {
            return BarnepensjonRevurderingRedigerbartUtfall(
                erEtterbetaling = etterbetaling != null,
            )
        }
    }
}
