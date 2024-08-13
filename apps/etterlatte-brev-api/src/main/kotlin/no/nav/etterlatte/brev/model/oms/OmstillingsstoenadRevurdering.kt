package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.model.BrevDataFerdigstilling
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.brev.model.toFeilutbetalingType
import no.nav.etterlatte.brev.model.vedleggHvisFeilutbetaling
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
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
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
    val harFlereUtbetalingsperioder: Boolean,
    val harUtbetaling: Boolean,
    val omsRettUtenTidsbegrensning: Boolean,
    val feilutbetaling: FeilutbetalingType,
) : BrevDataFerdigstilling {
    companion object {
        fun fra(
            innholdMedVedlegg: InnholdMedVedlegg,
            avkortingsinfo: Avkortingsinfo,
            forrigeAvkortingsinfo: Avkortingsinfo?,
            etterbetalingDTO: EtterbetalingDTO?,
            trygdetid: TrygdetidDto,
            brevutfall: BrevutfallDto,
            revurderingaarsak: Revurderingaarsak?,
            navnAvdoed: String,
            vilkaarsVurdering: VilkaarsvurderingDto,
        ): OmstillingsstoenadRevurdering {
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
                        sanksjon = it.sanksjon != null,
                        institusjon = it.institusjon != null,
                    )
                }

            val feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg))
            val sisteBeregningsperiode = beregningsperioder.maxBy { it.datoFOM }

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
                    erEndret(
                        forrigeAvkortingsinfo,
                        avkortingsinfo,
                    ) ||
                        revurderingaarsak == Revurderingaarsak.FRA_0UTBETALING_TIL_UTBETALING,
                erOmgjoering = revurderingaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                // TODO klage kobler seg på her
                datoVedtakOmgjoering = null,
                beregning =
                    OmstillingsstoenadBeregning(
                        innhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING),
                        virkningsdato = avkortingsinfo.virkningsdato,
                        beregningsperioder = beregningsperioder,
                        sisteBeregningsperiode = sisteBeregningsperiode,
                        trygdetid =
                            trygdetid.fromDto(
                                beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                                navnAvdoed = navnAvdoed,
                            ),
                    ),
                etterbetaling =
                    etterbetalingDTO?.let {
                        Etterbetaling.fraOmstillingsstoenadBeregningsperioder(
                            etterbetalingDTO,
                            beregningsperioder,
                        )
                    },
                harFlereUtbetalingsperioder = beregningsperioder.size > 1,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                feilutbetaling = feilutbetaling,
            )
        }

        private fun erEndret(
            forrigeAvkortingsinfo: Avkortingsinfo?,
            avkortingsinfo: Avkortingsinfo,
        ): Boolean {
            // Sjekker siste periode på forrige iverksatte og gjeldende behandling - mulig dette ikke holder
            // med litt mer komplekse behandlinger?
            val beloepForrigeBehandling = forrigeAvkortingsinfo?.beregningsperioder?.maxBy { it.datoFOM }?.utbetaltBeloep
            val beloepGjeldendeBehandling = avkortingsinfo.beregningsperioder.maxBy { it.datoFOM }.utbetaltBeloep
            return beloepForrigeBehandling == null || beloepForrigeBehandling != beloepGjeldendeBehandling
        }
    }
}

data class OmstillingsstoenadRevurderingRedigerbartUtfall(
    val feilutbetaling: FeilutbetalingType,
    val harUtbetaling: Boolean,
    val erEtterbetaling: Boolean,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            etterbetaling: EtterbetalingDTO?,
            brevutfall: BrevutfallDto,
        ): OmstillingsstoenadRevurderingRedigerbartUtfall =
            OmstillingsstoenadRevurderingRedigerbartUtfall(
                feilutbetaling = toFeilutbetalingType(requireNotNull(brevutfall.feilutbetaling?.valg)),
                harUtbetaling = avkortingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                erEtterbetaling = etterbetaling != null,
            )
    }
}
