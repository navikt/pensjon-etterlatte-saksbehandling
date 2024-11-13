package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.beregning.grunnlag.Reduksjon
import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class OmstillingsstoenadVedtakInntektsjusteringRedigerbartUtfall(
    val inntektsbeloep: Kroner,
    val opphoerDato: LocalDate?,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            opphoerDato: LocalDate?,
        ): OmstillingsstoenadVedtakInntektsjusteringRedigerbartUtfall {
            val sisteBeregningsperiode =
                avkortingsinfo.beregningsperioder
                    .filter {
                        it.datoFOM.year ==
                            avkortingsinfo.beregningsperioder
                                .first()
                                .datoFOM.year
                    }.maxBy { it.datoFOM }

            return OmstillingsstoenadVedtakInntektsjusteringRedigerbartUtfall(
                inntektsbeloep = sisteBeregningsperiode.inntekt,
                opphoerDato = opphoerDato,
            )
        }
    }
}

class OmstillingsstoenadInntektsjusteringVedtak(
    override val innhold: List<Slate.Element>,
    val beregning: OmstillingsstoenadBeregning,
    val omsRettUtenTidsbegrensning: Boolean = false,
    val tidligereFamiliepleier: Boolean = false,
    val inntektsaar: Int,
    val harUtbetaling: Boolean,
    val endringIUtbetaling: Boolean,
    val virkningstidspunkt: LocalDate,
    val bosattUtland: Boolean,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            avkortingsinfo: Avkortingsinfo,
            trygdetid: TrygdetidDto,
        ): OmstillingsstoenadInntektsjusteringVedtak {
            // TODO duplikater som bør vurderes å trekkes ut felles
            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map {
                    OmstillingsstoenadBeregningsperiode(
                        datoFOM = it.datoFOM,
                        datoTOM = it.datoTOM,
                        inntekt = it.inntekt,
                        oppgittInntekt = it.oppgittInntekt,
                        fratrekkInnAar = it.fratrekkInnAar,
                        innvilgaMaaneder = it.innvilgaMaaneder,
                        grunnbeloep = it.grunnbeloep,
                        ytelseFoerAvkorting = it.ytelseFoerAvkorting,
                        restanse = it.restanse,
                        utbetaltBeloep = it.utbetaltBeloep,
                        trygdetid = it.trygdetid,
                        beregningsMetodeAnvendt = it.beregningsMetodeAnvendt,
                        beregningsMetodeFraGrunnlag = it.beregningsMetodeFraGrunnlag,
                        sanksjon = it.sanksjon != null,
                        institusjon = it.institusjon != null && it.institusjon.reduksjon != Reduksjon.NEI_KORT_OPPHOLD,
                        erOverstyrtInnvilgaMaaneder = it.erOverstyrtInnvilgaMaaneder,
                    )
                }

            val sisteBeregningsperiode =
                beregningsperioder
                    .filter {
                        it.datoFOM.year == beregningsperioder.first().datoFOM.year
                    }.maxBy { it.datoFOM }

            return OmstillingsstoenadInntektsjusteringVedtak(
                innhold = innholdMedVedlegg.innhold(),
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING),
                        virkningsdato = LocalDate.of(2025, 1, 1),
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        sisteBeregningsperiodeNesteAar = null,
                        trygdetid =
                            trygdetid.fromDto(
                                beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                                navnAvdoed = "TODO", // TODO
                            ),
                        oppphoersdato = null,
                        opphoerNesteAar = false, // inntekt neste år ikke implementert for revurdering
                    ),
                omsRettUtenTidsbegrensning = false,
                tidligereFamiliepleier = false,
                inntektsaar = 2025,
                harUtbetaling = true,
                endringIUtbetaling = true,
                virkningstidspunkt = LocalDate.of(2025, 1, 1),
                bosattUtland = false,
            )
        }
    }
}

data class OmstillingsstoenadInntektsjusteringVarsel(
    val inntektsaar: Int,
    val bosattUtland: Boolean,
    val virkningstidspunkt: LocalDate,
) : BrevData {
    companion object {
        fun fra() =
            OmstillingsstoenadInntektsjusteringVarsel(
                // TODO
                inntektsaar = 2025,
                bosattUtland = false,
                virkningstidspunkt = LocalDate.of(2025, 1, 1),
            )
    }
}
