package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.erYrkesskade
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.time.YearMonth

data class OmstillingsstoenadVedtakInntektsjusteringRedigerbartUtfall(
    val inntektsbeloep: Kroner,
    val inntektsaar: Int,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            virkningstidspunkt: YearMonth,
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
                inntektsaar = virkningstidspunkt.year,
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
            vilkaarsVurdering: VilkaarsvurderingDto,
            behandling: DetaljertBehandling,
            landKodeverk: List<LandDto>,
        ): OmstillingsstoenadInntektsjusteringVedtak {
            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }
            val beregningsperioderOpphoer = utledBeregningsperioderOpphoer(behandling, beregningsperioder)
            val sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode
            val virk = behandling.virkningstidspunkt!!.dato.atDay(1)

            val omsRettUtenTidsbegrensning =
                vilkaarsVurdering.vilkaar
                    .single {
                        it.hovedvilkaar.type in
                            listOf(
                                VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                            )
                    }.hovedvilkaar.resultat == Utfall.OPPFYLT

            return OmstillingsstoenadInntektsjusteringVedtak(
                innhold = innholdMedVedlegg.innhold(),
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = emptyList(), // Skal ikke sende med noe vedlegg for denne brevtypen
                        virkningsdato = virk,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        sisteBeregningsperiodeNesteAar = null,
                        trygdetid =
                            trygdetid.fromDto(
                                beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                                navnAvdoed = null,
                                landKodeverk = landKodeverk,
                            ),
                        oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                        opphoerNesteAar =
                            beregningsperioderOpphoer.forventetOpphoerDato?.year == (behandling.virkningstidspunkt().dato.year + 1),
                        erYrkesskade = trygdetid.erYrkesskade(),
                    ),
                omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning,
                tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true,
                inntektsaar = virk.year,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                endringIUtbetaling = avkortingsinfo.endringIUtbetalingVedVirk,
                virkningstidspunkt = virk,
                bosattUtland = behandling.erBosattUtland(),
            )
        }
    }
}
