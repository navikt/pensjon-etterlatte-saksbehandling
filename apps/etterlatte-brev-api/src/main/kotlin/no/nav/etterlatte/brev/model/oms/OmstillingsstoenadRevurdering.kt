package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.TrygdetidMedBeregningsmetode
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.LavEllerIngenInntekt
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import java.time.LocalDate

data class OmstillingsstoenadRevurdering(
    val innhold: List<Slate.Element>,
    val erEndret: Boolean,
    val erOmgjoering: Boolean,
    val datoVedtakOmgjoering: LocalDate?,
    val beregning: OmstillingsstoenadBeregning,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
    val harFlereUtbetalingsperioder: Boolean,
    val harUtbetaling: Boolean,
    val lavEllerIngenInntekt: Boolean,
    val feilutbetaling: FeilutbetalingType,
) : BrevData() {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            avkortingsinfo: Avkortingsinfo,
            utbetalingsinfo: Utbetalingsinfo,
            forrigeUtbetalingsinfo: Utbetalingsinfo?,
            etterbetalingDTO: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            brevutfall: BrevutfallDto,
            revurderingaarsak: Revurderingaarsak?,
        ): OmstillingsstoenadRevurdering {
            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map {
                    OmstillingsstoenadBeregningsperiode(
                        datoFOM = it.datoFOM,
                        datoTOM = it.datoTOM,
                        inntekt = it.inntekt,
                        ytelseFoerAvkorting = it.ytelseFoerAvkorting,
                        utbetaltBeloep = it.utbetaltBeloep,
                        trygdetid = it.trygdetid,
                    )
                }

            return OmstillingsstoenadRevurdering(
                innhold = innholdMedVedlegg.innhold(),
                erEndret = forrigeUtbetalingsinfo == null || forrigeUtbetalingsinfo.beloep != utbetalingsinfo.beloep,
                erOmgjoering = revurderingaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                datoVedtakOmgjoering = null, // TODO klage kobler seg på her
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.BEREGNING_INNHOLD),
                        virkningsdato = avkortingsinfo.virkningsdato,
                        inntekt = avkortingsinfo.inntekt,
                        grunnbeloep = avkortingsinfo.grunnbeloep,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = beregningsperioder.maxBy { it.datoFOM },
                        trygdetid =
                            TrygdetidMedBeregningsmetode(
                                trygdetidsperioder = trygdetid.perioder,
                                beregnetTrygdetidAar = trygdetid.aarTrygdetid,
                                beregnetTrygdetidMaaneder = trygdetid.maanederTrygdetid,
                                prorataBroek = trygdetid.prorataBroek,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = trygdetid.mindreEnnFireFemtedelerAvOpptjeningstiden,
                                beregningsMetodeFraGrunnlag = utbetalingsinfo.beregningsperioder.first().beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = utbetalingsinfo.beregningsperioder.first().beregningsMetodeAnvendt,
                            ),
                    ),
                etterbetaling =
                    etterbetalingDTO?.let {
                        Etterbetaling.fraOmstillingsstoenadBeregningsperioder(
                            etterbetalingDTO,
                            beregningsperioder,
                        )
                    },
                harFlereUtbetalingsperioder = beregningsperioder.size > 1,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                lavEllerIngenInntekt = brevutfall.lavEllerIngenInntekt == LavEllerIngenInntekt.JA,
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
            )
        }
    }
}

data class OmstillingsstoenadRevurderingRedigerbartUtfall(
    val feilutbetaling: FeilutbetalingType,
    val harUtbetaling: Boolean,
    val harEtterbetaling: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: EtterbetalingDTO?,
            brevutfall: BrevutfallDto,
        ): OmstillingsstoenadRevurderingRedigerbartUtfall =
            OmstillingsstoenadRevurderingRedigerbartUtfall(
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
                harUtbetaling = avkortingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                harEtterbetaling = etterbetaling != null,
            )
    }
}

private fun toFeilutbetalingType(feilutbetalingValg: FeilutbetalingValg) =
    when (feilutbetalingValg) {
        FeilutbetalingValg.NEI -> FeilutbetalingType.INGEN_FEILUTBETALING
        FeilutbetalingValg.JA_INGEN_TK -> FeilutbetalingType.FEILUTBETALING_UTEN_VARSEL
        FeilutbetalingValg.JA_VARSEL -> FeilutbetalingType.FEILUTBETALING_MED_VARSEL
    }
