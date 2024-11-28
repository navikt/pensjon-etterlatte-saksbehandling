package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningRedigerbartUtfall
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import java.time.LocalDate

data class OmstillingsstoenadRevurdering(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    val erEndret: Boolean,
    val erOmgjoering: Boolean,
    val datoVedtakOmgjoering: LocalDate?,
    val beregning: OmstillingsstoenadBeregning,
    val omsRettUtenTidsbegrensning: Boolean,
    val feilutbetaling: FeilutbetalingType,
    val bosattUtland: Boolean,
) : BrevDataFerdigstilling {
    init {
        if (erOmgjoering && datoVedtakOmgjoering == null) {
            throw InternfeilException(
                "Kunne ikke lage revurderingsbrevet for omstillingsstønad siden vi ikke" +
                    " fikk dato vedtak for omgjøring, i en revurdering som er omgjøring.",
            )
        }
    }

    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            avkortingsinfo: Avkortingsinfo,
            trygdetid: TrygdetidDto,
            brevutfall: BrevutfallDto,
            revurderingaarsak: Revurderingaarsak?,
            navnAvdoed: String,
            vilkaarsVurdering: VilkaarsvurderingDto,
            datoVedtakOmgjoering: LocalDate?,
            utlandstilknytning: UtlandstilknytningType?,
            behandling: DetaljertBehandling,
        ): OmstillingsstoenadRevurdering {
            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }

            val feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg))
            val beregningsperioderOpphoer = utledBeregningsperioderOpphoer(behandling, beregningsperioder)
            val sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode

            val omsRettUtenTidsbegrensning =
                vilkaarsVurdering.vilkaar.single {
                    it.hovedvilkaar.type in
                        listOf(
                            VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                        )
                }

            return OmstillingsstoenadRevurdering(
                innhold = innholdMedVedlegg.innhold(),
                innholdForhaandsvarsel =
                    vedleggHvisFeilutbetaling(
                        feilutbetaling,
                        innholdMedVedlegg,
                        BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
                    ),
                erEndret =
                    avkortingsinfo.endringIUtbetalingVedVirk ||
                        revurderingaarsak == Revurderingaarsak.FRA_0UTBETALING_TIL_UTBETALING,
                erOmgjoering = revurderingaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                datoVedtakOmgjoering = datoVedtakOmgjoering,
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING),
                        virkningsdato = avkortingsinfo.virkningsdato,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
                        trygdetid =
                            trygdetid.fromDto(
                                beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                                navnAvdoed = navnAvdoed,
                            ),
                        oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                        opphoerNesteAar =
                            beregningsperioderOpphoer.forventetOpphoerDato?.year == (behandling.virkningstidspunkt().dato.year + 1),
                    ),
                omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                feilutbetaling = feilutbetaling,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
            )
        }
    }
}

data class OmstillingsstoenadRevurderingRedigerbartUtfall(
    val beregning: OmstillingsstoenadBeregningRedigerbartUtfall,
    val erEtterbetaling: Boolean,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
    val feilutbetaling: FeilutbetalingType,
    val harFlereUtbetalingsperioder: Boolean,
    val harUtbetaling: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            behandling: DetaljertBehandling,
            brevutfall: BrevutfallDto,
            etterbetaling: EtterbetalingDTO?,
        ): OmstillingsstoenadRevurderingRedigerbartUtfall {
            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }

            val beregningsperioderOpphoer = utledBeregningsperioderOpphoer(behandling, beregningsperioder)
            val sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode

            return OmstillingsstoenadRevurderingRedigerbartUtfall(
                beregning =
                    OmstillingsstoenadBeregningRedigerbartUtfall(
                        virkningsdato = avkortingsinfo.virkningsdato,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
                        oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                        opphoerNesteAar =
                            beregningsperioderOpphoer.forventetOpphoerDato?.year == (behandling.virkningstidspunkt().dato.year + 1),
                    ),
                erEtterbetaling = etterbetaling != null,
                etterbetaling =
                    etterbetaling?.let {
                        Etterbetaling.fraOmstillingsstoenadBeregningsperioder(
                            etterbetaling,
                            beregningsperioder,
                        )
                    },
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
                harFlereUtbetalingsperioder = beregningsperioder.size > 1,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
            )
        }
    }
}
