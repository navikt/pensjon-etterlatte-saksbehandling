package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.time.YearMonth

data class OmstillingsstoenadInnvilgelseDTO(
    val innhold: List<Slate.Element>,
    val avdoed: Avdoed,
    val beregning: OmstillingsstoenadBeregning,
    val innvilgetMindreEnnFireMndEtterDoedsfall: Boolean,
    val lavEllerIngenInntekt: Boolean,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo,
            etterbetalinginfo: EtterbetalingDTO?,
            trygdetid: Trygdetid,
            innholdMedVedlegg: InnholdMedVedlegg,
            lavEllerIngenInntekt: Boolean,
        ): OmstillingsstoenadInnvilgelseDTO {
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

            val avdoed = generellBrevData.personerISak.avdoede.minBy { it.doedsdato }

            return OmstillingsstoenadInnvilgelseDTO(
                innhold = innholdMedVedlegg.innhold(),
                avdoed = generellBrevData.personerISak.avdoede.minBy { it.doedsdato },
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.BEREGNING_INNHOLD),
                        virkningsdato = avkortingsinfo.virkningsdato,
                        inntekt = avkortingsinfo.inntekt,
                        grunnbeloep = avkortingsinfo.grunnbeloep,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = beregningsperioder.minByOrNull { it.datoFOM }!!,
                        trygdetid =
                            OmstillingsstoenadTrygdetid(
                                trygdetidsperioder = trygdetid.perioder,
                                beregnetTrygdetidAar = trygdetid.aarTrygdetid,
                                beregnetTrygdetidMaaneder = trygdetid.maanederTrygdetid,
                                prorataBroek = trygdetid.prorataBroek,
                                mindreEnnFireFemtedelerAvOpptjeningstiden = trygdetid.mindreEnnFireFemtedelerAvOpptjeningstiden,
                                beregningsMetodeFraGrunnlag = utbetalingsinfo.beregningsperioder.first().beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = utbetalingsinfo.beregningsperioder.first().beregningsMetodeAnvendt,
                            ),
                    ),
                innvilgetMindreEnnFireMndEtterDoedsfall =
                    avdoed.doedsdato
                        .plusMonths(4)
                        .isAfter(avkortingsinfo.virkningsdato),
                lavEllerIngenInntekt = lavEllerIngenInntekt,
                etterbetaling = OmstillingsstoenadEtterbetaling.fra(etterbetalinginfo, beregningsperioder),
            )
        }
    }
}

data class OmstillingsstoenadInnvilgelseRedigerbartUtfallDTO(
    val virkningsdato: LocalDate,
    val avdoed: Avdoed,
    val utbetalingsbeloep: Kroner,
    val etterbetaling: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: Boolean,
        ): OmstillingsstoenadInnvilgelseRedigerbartUtfallDTO =
            OmstillingsstoenadInnvilgelseRedigerbartUtfallDTO(
                virkningsdato = utbetalingsinfo.virkningsdato,
                avdoed = generellBrevData.personerISak.avdoede.minBy { it.doedsdato },
                utbetalingsbeloep = avkortingsinfo.beregningsperioder.first().utbetaltBeloep,
                etterbetaling = etterbetaling,
            )
    }
}

data class OmstillingsstoenadBeregning(
    val innhold: List<Slate.Element>,
    val virkningsdato: LocalDate,
    val inntekt: Kroner,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
    val sisteBeregningsperiode: OmstillingsstoenadBeregningsperiode,
    val trygdetid: OmstillingsstoenadTrygdetid,
)

data class OmstillingsstoenadBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val inntekt: Kroner,
    val ytelseFoerAvkorting: Kroner,
    val utbetaltBeloep: Kroner,
    val trygdetid: Int,
)

data class OmstillingsstoenadTrygdetid(
    val trygdetidsperioder: List<Trygdetidsperiode>,
    val beregnetTrygdetidAar: Int,
    val beregnetTrygdetidMaaneder: Int,
    val prorataBroek: IntBroek?,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
)

data class OmstillingsstoenadEtterbetaling(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
) {
    companion object {
        fun fra(
            dto: EtterbetalingDTO?,
            perioder: List<OmstillingsstoenadBeregningsperiode>,
        ) = if (dto == null) {
            null
        } else {
            OmstillingsstoenadEtterbetaling(
                fraDato = dto.datoFom,
                tilDato = dto.datoTom,
                beregningsperioder = perioder.filter { YearMonth.from(it.datoFOM) <= YearMonth.from(dto.datoTom) },
            )
        }
    }
}
