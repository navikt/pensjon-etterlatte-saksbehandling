package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.TrygdetidMedBeregningsmetode
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.LavEllerIngenInntekt
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class OmstillingsstoenadInnvilgelse(
    override val innhold: List<Slate.Element>,
    val avdoed: Avdoed,
    val beregning: OmstillingsstoenadBeregning,
    val innvilgetMindreEnnFireMndEtterDoedsfall: Boolean,
    val lavEllerIngenInntekt: Boolean,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            generellBrevData: GenerellBrevData,
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            brevutfall: BrevutfallDto,
        ): OmstillingsstoenadInnvilgelse {
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

            val avdoed = generellBrevData.personerISak.avdoede.minBy { it.doedsdato }
            val sisteBeregningsperiode = beregningsperioder.maxBy { it.datoFOM }

            return OmstillingsstoenadInnvilgelse(
                innhold = innholdMedVedlegg.innhold(),
                avdoed = generellBrevData.personerISak.avdoede.minBy { it.doedsdato },
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
                    ),
                innvilgetMindreEnnFireMndEtterDoedsfall =
                    avdoed.doedsdato
                        .plusMonths(4)
                        .isAfter(avkortingsinfo.virkningsdato),
                lavEllerIngenInntekt = brevutfall.lavEllerIngenInntekt == LavEllerIngenInntekt.JA,
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraOmstillingsstoenadBeregningsperioder(dto, beregningsperioder) },
            )
        }
    }
}

data class OmstillingsstoenadInnvilgelseRedigerbartUtfall(
    val virkningsdato: LocalDate,
    val avdoed: Avdoed,
    val utbetalingsbeloep: Kroner,
    val etterbetaling: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: EtterbetalingDTO?,
        ): OmstillingsstoenadInnvilgelseRedigerbartUtfall =
            OmstillingsstoenadInnvilgelseRedigerbartUtfall(
                virkningsdato = utbetalingsinfo.virkningsdato,
                avdoed = generellBrevData.personerISak.avdoede.minBy { it.doedsdato },
                utbetalingsbeloep = avkortingsinfo.beregningsperioder.first().utbetaltBeloep,
                etterbetaling = etterbetaling != null,
            )
    }
}
