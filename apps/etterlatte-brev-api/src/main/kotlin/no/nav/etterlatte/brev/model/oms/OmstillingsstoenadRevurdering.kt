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
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class OmstillingsstoenadRevurderingData(
    val erEndret: Boolean,
    val erOmgjoering: Boolean,
    val datoVedtakOmgjoering: LocalDate?,
    val beregning: OmstillingsstoenadBeregning,
    val omsRettUtenTidsbegrensning: Boolean,
    val feilutbetaling: FeilutbetalingType,
    val bosattUtland: Boolean,
    val erInnvilgelsesaar: Boolean,
    val tidligereFamiliepleier: Boolean,
)

data class OmstillingsstoenadRevurdering(
    override val innhold: List<Slate.Element>,
    val innholdForhaandsvarsel: List<Slate.Element>,
    override val data: OmstillingsstoenadRevurderingData,
) : BrevDataFerdigstilling {
    init {
        if (data.erOmgjoering && data.datoVedtakOmgjoering == null) {
            throw InternfeilException(
                "Kunne ikke lage revurderingsbrevet for omstillingsstønad siden vi ikke" +
                    " fikk dato vedtak for omgjøring, i en revurdering som er omgjøring.",
            )
        }
    }

    companion object {
        private fun innholdMedVedlegg( // TODO betingelser?
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

data class OmstillingsstoenadRevurderingRedigerbartUtfallData(
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
)

data class OmstillingsstoenadRevurderingRedigerbartUtfall(
    override val data: OmstillingsstoenadRevurderingRedigerbartUtfallData,
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
                data =
                    OmstillingsstoenadRevurderingRedigerbartUtfallData(
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
                    ),
            )
        }
    }
}
