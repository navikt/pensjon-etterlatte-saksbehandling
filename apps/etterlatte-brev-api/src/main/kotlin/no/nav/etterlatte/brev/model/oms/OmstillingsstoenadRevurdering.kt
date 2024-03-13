package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.InntektendringType
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.TrygdetidMedBeregningsmetode
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.LavEllerIngenInntekt
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import java.time.LocalDate

data class OmstillingsstoenadRevurdering(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    val erEndret: Boolean,
    val erOmgjoering: Boolean,
    val datoVedtakOmgjoering: LocalDate?,
    val beregning: OmstillingsstoenadBeregning,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
    val harFlereUtbetalingsperioder: Boolean,
    val harUtbetaling: Boolean,
    val lavEllerIngenInntekt: Boolean,
    val feilutbetaling: FeilutbetalingType,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            avkortingsinfo: Avkortingsinfo,
            forrigeAvkortingsinfo: Avkortingsinfo?,
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
                        aarsinntekt = it.aarsinntekt,
                        fratrekkInnAar = it.fratrekkInnAar,
                        relevantMaanederInnAar = it.relevanteMaanederInnAar,
                        grunnbeloep = it.grunnbeloep,
                        ytelseFoerAvkorting = it.ytelseFoerAvkorting,
                        restanse = it.restanse,
                        utbetaltBeloep = it.utbetaltBeloep,
                        trygdetid = it.trygdetid,
                        beregningsMetodeFraGrunnlag = it.beregningsMetodeFraGrunnlag,
                        beregningsMetodeAnvendt = it.beregningsMetodeAnvendt,
                    )
                }

            val feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg))
            val sisteBeregningsperiode = beregningsperioder.maxBy { it.datoFOM }

            return OmstillingsstoenadRevurdering(
                innhold = innholdMedVedlegg.innhold(),
                innholdForhaandsvarsel =
                    vedleggHvisFeilutbetaling(
                        feilutbetaling,
                        innholdMedVedlegg,
                        BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                    ),
                erEndret = erEndret(forrigeAvkortingsinfo, avkortingsinfo),
                erOmgjoering = revurderingaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                // TODO klage kobler seg på her
                datoVedtakOmgjoering = null,
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING),
                        virkningsdato = avkortingsinfo.virkningsdato,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        trygdetid =
                            TrygdetidMedBeregningsmetode(
                                trygdetidsperioder = trygdetid.perioder,
                                beregnetTrygdetidAar = trygdetid.aarTrygdetid,
                                beregnetTrygdetidMaaneder = trygdetid.maanederTrygdetid,
                                prorataBroek = trygdetid.prorataBroek,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = trygdetid.mindreEnnFireFemtedelerAvOpptjeningstiden,
                                beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                            ),
                        inntektendringType = inntektstype(forrigeAvkortingsinfo, avkortingsinfo),
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
                feilutbetaling = feilutbetaling,
            )
        }

        private fun erEndret(
            forrigeAvkortingsinfo: Avkortingsinfo?,
            avkortingsinfo: Avkortingsinfo,
        ): Boolean {
            // Sjekker siste periode på forrige iverksatte og gjeldende behandling - mulig dette ikke holder
            // med litt mer komplekse behandlinger?
            val beloepForrigeBehandling = forrigeAvkortingsinfo?.beregningsperioder?.maxBy { it.datoFOM }?.utbetaltBeloep
            val beloepGjeldendeBehandling = avkortingsinfo.beregningsperioder.maxBy { it.datoFOM }.utbetaltBeloep
            return beloepForrigeBehandling == null || beloepForrigeBehandling != beloepGjeldendeBehandling
        }

        private fun inntektstype(
            forrigeAvkortingsinfo: Avkortingsinfo?,
            avkortingsinfo: Avkortingsinfo,
        ): InntektendringType {
            if (forrigeAvkortingsinfo == null) {
                return InntektendringType.INGEN_ENDRING
            }
            val nyInntekt = avkortingsinfo.beregningsperioder.maxBy { it.datoFOM }.inntekt.value
            val forrigeInntekt = forrigeAvkortingsinfo.beregningsperioder.maxBy { it.datoFOM }.inntekt.value
            return if (nyInntekt < forrigeInntekt) {
                InntektendringType.MINDRE_INNTEKT
            } else if (nyInntekt > forrigeInntekt) {
                InntektendringType.MER_INNTEKT
            } else {
                InntektendringType.INGEN_ENDRING
            }
        }
    }
}

data class OmstillingsstoenadRevurderingRedigerbartUtfall(
    val feilutbetaling: FeilutbetalingType,
    val harUtbetaling: Boolean,
    val erEtterbetaling: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: EtterbetalingDTO?,
            brevutfall: BrevutfallDto,
        ): OmstillingsstoenadRevurderingRedigerbartUtfall =
            OmstillingsstoenadRevurderingRedigerbartUtfall(
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
                harUtbetaling = avkortingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                erEtterbetaling = etterbetaling != null,
            )
    }
}
