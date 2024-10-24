package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class OmstillingsstoenadInnvilgelse(
    override val innhold: List<Slate.Element>,
    val avdoed: Avdoed,
    val beregning: OmstillingsstoenadBeregning,
    val innvilgetMindreEnnFireMndEtterDoedsfall: Boolean,
    val omsRettUtenTidsbegrensning: Boolean,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
    val harUtbetaling: Boolean,
    val bosattUtland: Boolean,
    val erSluttbehandling: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: EtterbetalingDTO?,
            trygdetid: TrygdetidDto,
            vilkaarsVurdering: VilkaarsvurderingDto,
            avdoede: List<Avdoed>,
            utlandstilknytning: UtlandstilknytningType?,
            erSluttbehandling: Boolean,
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
                        beregningsMetodeAnvendt = it.beregningsMetodeAnvendt,
                        beregningsMetodeFraGrunnlag = it.beregningsMetodeFraGrunnlag,
                        sanksjon = it.sanksjon != null,
                        institusjon = it.institusjon != null && it.institusjon.reduksjon != Reduksjon.NEI_KORT_OPPHOLD,
                    )
                }

            val avdoed = avdoede.single()
            val sisteBeregningsperiode =
                beregningsperioder
                    .filter {
                        it.datoFOM.year == beregningsperioder.first().datoFOM.year
                    }.maxBy { it.datoFOM }
            val sisteBeregningsperiodeNesteAar =
                beregningsperioder
                    .filter {
                        it.datoFOM.year == beregningsperioder.first().datoFOM.year + 1
                    }.maxByOrNull { it.datoFOM }

            val omsRettUtenTidsbegrensning =
                vilkaarsVurdering.vilkaar.single {
                    it.hovedvilkaar.type in
                        listOf(
                            VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                        )
                }

            return OmstillingsstoenadInnvilgelse(
                innhold = innholdMedVedlegg.innhold(),
                avdoed = avdoede.minBy { it.doedsdato },
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING),
                        virkningsdato = avkortingsinfo.virkningsdato,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        sisteBeregningsperiodeNesteAar = sisteBeregningsperiodeNesteAar,
                        trygdetid =
                            trygdetid.fromDto(
                                beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                                navnAvdoed = avdoed.navn,
                            ),
                    ),
                innvilgetMindreEnnFireMndEtterDoedsfall =
                    avdoed.doedsdato
                        .plusMonths(4)
                        .isAfter(avkortingsinfo.virkningsdato),
                omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraOmstillingsstoenadBeregningsperioder(dto, beregningsperioder) },
                erSluttbehandling = erSluttbehandling,
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
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: EtterbetalingDTO?,
            avdoede: List<Avdoed>,
        ): OmstillingsstoenadInnvilgelseRedigerbartUtfall =
            OmstillingsstoenadInnvilgelseRedigerbartUtfall(
                virkningsdato = utbetalingsinfo.virkningsdato,
                avdoed = avdoede.minBy { it.doedsdato },
                utbetalingsbeloep =
                    avkortingsinfo.beregningsperioder.firstOrNull()?.utbetaltBeloep
                        ?: throw UgyldigForespoerselException(
                            "MANGLER_BEREGNINGSPERIODER_AVKORTING",
                            "Mangler beregningsperioder i avkorting",
                        ),
                etterbetaling = etterbetaling != null,
            )
    }
}
