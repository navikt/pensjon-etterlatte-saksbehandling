package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningRedigerbartUtfall
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.brev.model.erYrkesskade
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.pensjon.brevbaker.api.model.Kroner
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
    val erInnvilgelsesaar: Boolean,
    val tidligereFamiliepleier: Boolean,
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
            vilkaarsVurdering: VilkaarsvurderingDto,
            datoVedtakOmgjoering: LocalDate?,
            utlandstilknytning: UtlandstilknytningType?,
            behandling: DetaljertBehandling,
            landKodeverk: List<LandDto>,
        ): OmstillingsstoenadRevurdering {
            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }

            val feilutbetaling =
                krevIkkeNull(brevutfall.feilutbetaling?.valg?.let(::toFeilutbetalingType)) {
                    "Feilutbetaling mangler i brevutfall"
                }
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
                        innhold = innholdMedVedlegg(innholdMedVedlegg, behandling),
                        virkningsdato = avkortingsinfo.virkningsdato,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
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
                omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                feilutbetaling = feilutbetaling,
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                erInnvilgelsesaar = avkortingsinfo.erInnvilgelsesaar,
                tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true,
            )
        }

        private fun innholdMedVedlegg(
            innholdMedVedlegg: InnholdMedVedlegg,
            behandling: DetaljertBehandling,
        ): List<Slate.Element> {
            if (behandling.revurderingsaarsak == Revurderingaarsak.INNTEKTSENDRING && behandling.prosesstype == Prosesstype.AUTOMATISK) {
                return emptyList()
            }
            return innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING)
        }
    }
}

data class OmstillingsstoenadRevurderingRedigerbartUtfall(
    val beregning: OmstillingsstoenadBeregningRedigerbartUtfall,
    val erEndret: Boolean,
    val erEtterbetaling: Boolean,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
    val feilutbetaling: FeilutbetalingType,
    val harFlereUtbetalingsperioder: Boolean,
    val harUtbetaling: Boolean,
    val inntekt: Kroner,
    val inntektsAar: Int,
    val mottattInntektendringAutomatisk: LocalDate?,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            behandling: DetaljertBehandling,
            brevutfall: BrevutfallDto,
            etterbetaling: EtterbetalingDTO?,
            revurderingaarsak: Revurderingaarsak?,
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
                erEndret =
                    avkortingsinfo.endringIUtbetalingVedVirk ||
                        revurderingaarsak == Revurderingaarsak.FRA_0UTBETALING_TIL_UTBETALING,
                erEtterbetaling = etterbetaling != null,
                etterbetaling =
                    etterbetaling?.let {
                        Etterbetaling.fraOmstillingsstoenadBeregningsperioder(
                            etterbetaling,
                            beregningsperioder,
                        )
                    },
                feilutbetaling =
                    krevIkkeNull(brevutfall.feilutbetaling?.valg?.let(::toFeilutbetalingType)) {
                        "Feilutbetaling mangler i brevutfall"
                    },
                harFlereUtbetalingsperioder = beregningsperioder.size > 1,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                inntekt = sisteBeregningsperiode.inntekt,
                inntektsAar = sisteBeregningsperiode.datoFOM.year,
                mottattInntektendringAutomatisk =
                    if (behandling.prosesstype == Prosesstype.AUTOMATISK &&
                        behandling.revurderingsaarsak == Revurderingaarsak.INNTEKTSENDRING
                    ) {
                        behandling.mottattDato?.toLocalDate()
                            ?: throw InternfeilException("Automatisk inntektsendring må ha mottatt dato")
                    } else {
                        null
                    },
            )
        }
    }
}
