package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataFerdigstilling
import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class OmstillingsstoenadInnvilgelse(
    override val innhold: List<Slate.Element>,
    val beregning: OmstillingsstoenadBeregning,
    val innvilgetMindreEnnFireMndEtterDoedsfall: Boolean,
    val omsRettUtenTidsbegrensning: Boolean,
    val etterbetaling: OmstillingsstoenadEtterbetaling?,
    val harUtbetaling: Boolean,
    val bosattUtland: Boolean,
    val erSluttbehandling: Boolean,
    val tidligereFamiliepleier: Boolean,
    val datoVedtakOmgjoering: LocalDate?,
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
            landKodeverk: List<LandDto>,
            klage: Klage?,
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

            val doedsdatoEllerOpphoertPleieforhold =
                if (erTidligereFamiliepleier) {
                    behandling.tidligereFamiliepleier!!.opphoertPleieforhold!!
                } else {
                    avdoede.single().doedsdato
                }

            return OmstillingsstoenadInnvilgelse(
                innhold = innholdMedVedlegg.innhold(),
                beregning =
                    omsBeregning(
                        vedleggInnhold = innholdMedVedlegg.finnVedlegg(BrevVedleggKey.OMS_BEREGNING),
                        behandling = behandling,
                        trygdetid = trygdetid,
                        avkortingsinfo = avkortingsinfo,
                        landKodeverk = landKodeverk,
                    ),
                innvilgetMindreEnnFireMndEtterDoedsfall =
                    innvilgetMindreEnnFireMndEtterDoedsfall(
                        doedsdatoEllerOpphoertPleieforhold = doedsdatoEllerOpphoertPleieforhold,
                    ),
                omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                bosattUtland = utlandstilknytning == UtlandstilknytningType.BOSATT_UTLAND,
                etterbetaling =
                    etterbetaling
                        ?.let { dto -> Etterbetaling.fraOmstillingsstoenadBeregningsperioder(dto, beregningsperioder) },
                erSluttbehandling = behandling.erSluttbehandling,
                tidligereFamiliepleier = erTidligereFamiliepleier,
                datoVedtakOmgjoering = klage?.datoVedtakOmgjoering(),
            )
        }

        fun innvilgetMindreEnnFireMndEtterDoedsfall(
            innvilgelsesDato: LocalDate = LocalDate.now(),
            doedsdatoEllerOpphoertPleieforhold: LocalDate,
        ): Boolean = innvilgelsesDato.isBefore(doedsdatoEllerOpphoertPleieforhold.plusMonths(4))
    }
}

data class OmstillingsstoenadInnvilgelseRedigerbartUtfall(
    val virkningsdato: LocalDate,
    val utbetalingsbeloep: Kroner,
    val etterbetaling: Boolean,
    val avdoed: Avdoed?,
    val harUtbetaling: Boolean,
    val beregning: OmstillingsstoenadBeregning,
    val erSluttbehandling: Boolean = false,
    val tidligereFamiliepleier: Boolean,
    val datoVedtakOmgjoering: LocalDate?,
) : BrevDataRedigerbar {
    companion object {
        fun fra(
            avkortingsinfo: Avkortingsinfo,
            behandling: DetaljertBehandling,
            etterbetaling: EtterbetalingDTO?,
            tidligereFamiliepleier: Boolean,
            klage: Klage?,
            erSluttbehandling: Boolean?,
            avdoede: List<Avdoed>,
            trygdetid: TrygdetidDto,
        ): OmstillingsstoenadInnvilgelseRedigerbartUtfall =
            OmstillingsstoenadInnvilgelseRedigerbartUtfall(
                virkningsdato = avkortingsinfo.virkningsdato,
                utbetalingsbeloep =
                    avkortingsinfo.beregningsperioder.firstOrNull()?.utbetaltBeloep
                        ?: throw UgyldigForespoerselException(
                            "MANGLER_BEREGNINGSPERIODER_AVKORTING",
                            "Mangler beregningsperioder i avkorting",
                        ),
                etterbetaling = etterbetaling != null,
                tidligereFamiliepleier = tidligereFamiliepleier,
                datoVedtakOmgjoering = klage?.datoVedtakOmgjoering(),
                avdoed = if (tidligereFamiliepleier) null else avdoede.single(),
                erSluttbehandling = erSluttbehandling ?: false,
                harUtbetaling = avkortingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                beregning =
                    omsBeregning(
                        vedleggInnhold = emptyList(),
                        behandling = behandling,
                        trygdetid = trygdetid,
                        avkortingsinfo = avkortingsinfo,
                        landKodeverk = emptyList(),
                    ),
            )
    }
}
