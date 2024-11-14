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
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
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
            behandling: DetaljertBehandling,
            navnAvdoed: String,
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

            val virk = behandling.virkningstidspunkt!!.dato.atDay(1)

            return OmstillingsstoenadInntektsjusteringVedtak(
                innhold = innholdMedVedlegg.innhold(),
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING),
                        virkningsdato = virk,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        sisteBeregningsperiodeNesteAar = null,
                        trygdetid =
                            trygdetid.fromDto(
                                beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                                navnAvdoed = navnAvdoed,
                            ),
                        oppphoersdato = behandling.opphoerFraOgMed?.atDay(1),
                        opphoerNesteAar = false, // inntekt neste år ikke implementert for revurdering
                    ),
                omsRettUtenTidsbegrensning = false, // TODO
                tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true,
                inntektsaar = virk.year,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                endringIUtbetaling = true, // TODO
                virkningstidspunkt = virk,
                bosattUtland = behandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
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
        fun fra(behandling: DetaljertBehandling): OmstillingsstoenadInntektsjusteringVarsel {
            val virk = behandling.virkningstidspunkt!!.dato
            return OmstillingsstoenadInntektsjusteringVarsel(
                inntektsaar = virk.year,
                bosattUtland = behandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                virkningstidspunkt = virk.atDay(1),
            )
        }
    }
}
