package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BarnepensjonBeregning
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.ManglerFrivilligSkattetrekk
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import java.time.LocalDate

data class BarnepensjonRevurdering(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    val erEndret: Boolean,
    val erOmgjoering: Boolean,
    val datoVedtakOmgjoering: LocalDate?,
    val beregning: BarnepensjonBeregning,
    val etterbetaling: EtterbetalingDTO?,
    val frivilligSkattetrekk: Boolean,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val kunNyttRegelverk: Boolean,
    val harFlereUtbetalingsperioder: Boolean,
    val harUtbetaling: Boolean,
    val feilutbetaling: FeilutbetalingType,
    val erMigrertYrkesskade: Boolean,
    val erEtterbetaling: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innhold: InnholdMedVedlegg,
            utbetalingsinfo: Utbetalingsinfo,
            forrigeUtbetalingsinfo: Utbetalingsinfo?,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: List<TrygdetidDto>,
            grunnbeloep: Grunnbeloep,
            utlandstilknytning: UtlandstilknytningType?,
            brevutfall: BrevutfallDto,
            revurderingaarsak: Revurderingaarsak?,
            erForeldreloes: Boolean,
            avdoede: List<Avdoed>,
            datoVedtakOmgjoering: LocalDate?,
            erMigrertYrkesskade: Boolean,
            landKodeverk: List<LandDto>,
        ): BarnepensjonRevurdering {
            val feilutbetaling =
                krevIkkeNull(brevutfall.feilutbetaling?.valg?.let(::toFeilutbetalingType)) {
                    "Feilutbetaling mangler i brevutfall"
                }

            return BarnepensjonRevurdering(
                innhold = innhold.innhold(),
                beregning =
                    barnepensjonBeregning(
                        innhold,
                        avdoede,
                        utbetalingsinfo,
                        grunnbeloep,
                        trygdetid,
                        erForeldreloes,
                        landKodeverk,
                    ),
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                brukerUnder18Aar = brevutfall.aldersgruppe == Aldersgruppe.UNDER_18,
                datoVedtakOmgjoering = datoVedtakOmgjoering,
                erEndret = forrigeUtbetalingsinfo == null || forrigeUtbetalingsinfo.beloep != utbetalingsinfo.beloep,
                erMigrertYrkesskade = erMigrertYrkesskade,
                erOmgjoering = revurderingaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                etterbetaling = etterbetaling,
                feilutbetaling = feilutbetaling,
                frivilligSkattetrekk = brevutfall.frivilligSkattetrekk ?: false,
                harFlereUtbetalingsperioder = utbetalingsinfo.beregningsperioder.size > 1,
                harUtbetaling = utbetalingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                innholdForhaandsvarsel =
                    vedleggHvisFeilutbetaling(
                        feilutbetaling,
                        innhold,
                        BrevVedleggKey.BP_FORHAANDSVARSEL_FEILUTBETALING,
                    ),
                kunNyttRegelverk =
                    utbetalingsinfo.beregningsperioder.all {
                        it.datoFOM.isAfter(BarnepensjonInnvilgelse.tidspunktNyttRegelverk) ||
                            it.datoFOM.isEqual(
                                BarnepensjonInnvilgelse.tidspunktNyttRegelverk,
                            )
                    },
                erEtterbetaling = etterbetaling != null,
            )
        }
    }
}

data class BarnepensjonRevurderingRedigerbartUtfall(
    val erEtterbetaling: Boolean,
    val harUtbetaling: Boolean,
    val feilutbetaling: FeilutbetalingType,
    val brukerUnder18Aar: Boolean,
    val bosattUtland: Boolean,
    val frivilligSkattetrekk: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            etterbetaling: EtterbetalingDTO?,
            utbetalingsinfo: Utbetalingsinfo,
            brevutfall: BrevutfallDto,
            utlandstilknytning: UtlandstilknytningType?,
        ): BarnepensjonRevurderingRedigerbartUtfall {
            val frivilligSkattetrekk =
                brevutfall.frivilligSkattetrekk ?: throw ManglerFrivilligSkattetrekk(brevutfall.behandlingId)

            return BarnepensjonRevurderingRedigerbartUtfall(
                erEtterbetaling = etterbetaling != null,
                harUtbetaling = utbetalingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                feilutbetaling =
                    krevIkkeNull(brevutfall.feilutbetaling?.valg?.let(::toFeilutbetalingType)) {
                        "Feilutbetaling mangler i brevutfall"
                    },
                brukerUnder18Aar =
                    krevIkkeNull(brevutfall.aldersgruppe) {
                        "Aldersgruppe mangler i brevutfall"
                    } == Aldersgruppe.UNDER_18,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                frivilligSkattetrekk = frivilligSkattetrekk,
            )
        }
    }
}
