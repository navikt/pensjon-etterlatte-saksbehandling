package no.nav.etterlatte.brev.model.oms

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
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class OmstillingsstoenadInnvilgelse(
    override val innhold: List<Slate.Element>,
    val avdoed: Avdoed?,
    val beregning: OmstillingsstoenadBeregning,
    val innvilgetMindreEnnFireMndEtterDoedsfall: Boolean,
    val omsRettUtenTidsbegrensning: Boolean,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
    val harUtbetaling: Boolean,
    val bosattUtland: Boolean,
    val erSluttbehandling: Boolean,
    val tidligereFamiliepleier: Boolean,
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
            behandling: DetaljertBehandling,
        ): OmstillingsstoenadInnvilgelse {
            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }

            val erTidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true

            val omsRettUtenTidsbegrensning =
                vilkaarsVurdering.vilkaar.single {
                    it.hovedvilkaar.type in
                        listOf(
                            VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                        )
                }

            val beregningsperioderOpphoer = utledBeregningsperioderOpphoer(behandling, beregningsperioder)
            val avdoed =
                if (erTidligereFamiliepleier) {
                    null
                } else {
                    avdoede.minBy { it.doedsdato }
                }

            val doedsdatoEllerOpphoertPleieforhold =
                if (erTidligereFamiliepleier) {
                    behandling.tidligereFamiliepleier!!.opphoertPleieforhold!!
                } else {
                    avdoede.single().doedsdato
                }

            return OmstillingsstoenadInnvilgelse(
                innhold = innholdMedVedlegg.innhold(),
                avdoed = avdoed,
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING),
                        virkningsdato = avkortingsinfo.virkningsdato,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode,
                        sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
                        trygdetid =
                            trygdetid.fromDto(
                                beregningsMetodeFraGrunnlag = beregningsperioderOpphoer.sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = beregningsperioderOpphoer.sisteBeregningsperiode.beregningsMetodeAnvendt,
                                navnAvdoed = null,
                            ),
                        oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                        opphoerNesteAar =
                            beregningsperioderOpphoer.forventetOpphoerDato?.year == (behandling.virkningstidspunkt().dato.year + 1),
                    ),
                innvilgetMindreEnnFireMndEtterDoedsfall =
                    doedsdatoEllerOpphoertPleieforhold
                        .plusMonths(4)
                        .isAfter(avkortingsinfo.virkningsdato),
                omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraOmstillingsstoenadBeregningsperioder(dto, beregningsperioder) },
                erSluttbehandling = behandling.erSluttbehandling,
                tidligereFamiliepleier = erTidligereFamiliepleier,
            )
        }
    }
}

data class OmstillingsstoenadInnvilgelseRedigerbartUtfall(
    val virkningsdato: LocalDate,
    val utbetalingsbeloep: Kroner,
    val etterbetaling: Boolean,
    val tidligereFamiliepleier: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            utbetalingsinfo: Utbetalingsinfo,
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: EtterbetalingDTO?,
            tidligereFamiliepleier: Boolean,
        ): OmstillingsstoenadInnvilgelseRedigerbartUtfall =
            OmstillingsstoenadInnvilgelseRedigerbartUtfall(
                virkningsdato = utbetalingsinfo.virkningsdato,
                utbetalingsbeloep =
                    avkortingsinfo.beregningsperioder.firstOrNull()?.utbetaltBeloep
                        ?: throw UgyldigForespoerselException(
                            "MANGLER_BEREGNINGSPERIODER_AVKORTING",
                            "Mangler beregningsperioder i avkorting",
                        ),
                etterbetaling = etterbetaling != null,
                tidligereFamiliepleier = tidligereFamiliepleier,
            )
    }
}
