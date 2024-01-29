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
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType

data class BarnepensjonRevurderingDTO(
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
            brukerUnder18Aar: Boolean,
        ): BrevData {
            val beregningsperioder = barnepensjonBeregningsperiodes(utbetalingsinfo)

            return BarnepensjonRevurderingDTO(
                innhold = innhold.innhold(),
                erEndret = forrigeUtbetalingsinfo == null || forrigeUtbetalingsinfo.beloep != utbetalingsinfo.beloep,
                beregning = barnepensjonBeregning(innhold, utbetalingsinfo, grunnbeloep, beregningsperioder, trygdetid),
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraBarnepensjonBeregningsperioder(dto, beregningsperioder) },
                brukerUnder18Aar = brukerUnder18Aar,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                harFlereUtbetalingsperioder = utbetalingsinfo.beregningsperioder.size > 1,
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(BarnepensjonInnvilgelseDTO.tidspunktNyttRegelverk) ||
                            it.datoFOM.isEqual(
                                BarnepensjonInnvilgelseDTO.tidspunktNyttRegelverk,
                            )
                    },
            )
        }
    }
}

data class BarnepensjonRevurderingRedigerbartUtfallDTO(
    val erEtterbetaling: Boolean,
) : EndringBrevData() {
    companion object {
        fun fra(etterbetaling: EtterbetalingDTO?): BrevData {
            return BarnepensjonRevurderingRedigerbartUtfallDTO(
                erEtterbetaling = etterbetaling != null,
            )
        }
    }
}
