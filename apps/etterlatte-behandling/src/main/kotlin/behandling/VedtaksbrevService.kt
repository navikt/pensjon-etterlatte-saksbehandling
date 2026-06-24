package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoService
import no.nav.etterlatte.behandling.klage.KlageService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.ManglerBrevutfall
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentVergeForSak
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.erYrkesskade
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseVedtakBrevData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurderingVedtakBrevData
import no.nav.etterlatte.brev.model.oms.omsBeregning
import no.nav.etterlatte.brev.model.oms.toAvkortetBeregningsperiode
import no.nav.etterlatte.brev.model.oms.utledBeregningsperioderOpphoer
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.kodeverk.KodeverkService
import no.nav.etterlatte.libs.common.behandling.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.etterlatte.libs.common.behandling.BehandlingType.REVURDERING
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak.NY_SOEKNAD
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak.OMGJOERING_ETTER_KLAGE
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import java.time.LocalDate
import java.util.UUID

class VedtaksbrevService(
    private val grunnlagService: GrunnlagService,
    private val vedtakInternalService: VedtakInternalService,
    private val brevKlient: BrevKlient,
    private val behandlingService: BehandlingService,
    private val beregningKlient: BeregningKlient,
    private val behandlingInfoService: BehandlingInfoService,
    private val trygdetidKlient: TrygdetidKlient,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val sakService: SakService,
    private val klageService: KlageService,
    private val kodeverkService: KodeverkService,
) {
    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev {
        val brevRequest = retryOgPakkUt { utledBrevRequest(bruker, behandlingId) }

        return brevKlient.opprettStrukturertBrev(
            behandlingId,
            brevRequest,
            bruker,
        )
    }

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
        skalLagres: Boolean,
    ): Pdf {
        val brevRequest = retryOgPakkUt { utledBrevRequest(bruker, behandlingId, skalLagres) }

        return brevKlient.genererPdf(brevID, behandlingId, brevRequest, bruker)
    }

    suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): BrevPayload {
        val brevRequest = retryOgPakkUt { utledBrevRequest(bruker, behandlingId) }

        return brevKlient.tilbakestillStrukturertBrev(
            brevID,
            behandlingId,
            brevRequest,
            bruker,
        )
    }

    suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        brevKlient.ferdigstillStrukturertBrev(behandlingId, Brevtype.VEDTAK, brukerTokenInfo)
    }

    suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev? = brevKlient.hentVedtaksbrev(behandlingId, bruker)

    private suspend fun utledBrevRequest(
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
        skalLagres: Boolean = false,
    ): BrevRequest =
        coroutineScope {
            val behandling =
                behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo)
                    ?: throw InternfeilException("Fant ikke behandlingId=$behandlingId")

            val vedtak =
                vedtakInternalService.hentVedtak(behandlingId, brukerTokenInfo)
                    ?: throw InternfeilException("Fant ikke vedtak for behandlingId=$behandlingId")

            if ((behandling.behandlingType == FØRSTEGANGSBEHANDLING && vedtak.type == VedtakType.INNVILGELSE) ||
                (behandling.behandlingType == REVURDERING && behandling.revurderingsaarsak == NY_SOEKNAD)
            ) {
                utledBrevRequestInnvilgelse(brukerTokenInfo, behandling, vedtak, skalLagres)
            } else {
                utledBrevRequestRevurdering(brukerTokenInfo, behandling, vedtak, skalLagres)
            }
        }

    private suspend fun utledBrevRequestInnvilgelse(
        brukerTokenInfo: BrukerTokenInfo,
        behandling: DetaljertBehandling,
        vedtak: VedtakDto,
        skalLagres: Boolean = false,
    ): BrevRequest {
        val behandlingId = behandling.id
        return coroutineScope {
            val alleLand = async { kodeverkService.hentAlleLand(brukerTokenInfo) }

            val sak = krevIkkeNull(sakService.finnSak(behandling.sak)) { "Sak må finnes" }

            val avkorting = beregningKlient.hentBeregningOgAvkorting(behandlingId, brukerTokenInfo)

            val vilkaarsvurdering =
                krevIkkeNull(vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)) { "Må ha vilkårsvurdering" }

            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlag(behandling.id)
                    ?: throw InternfeilException("Fant ikke grunnlag for behandlingId=${behandling.id}")

            val virkningsdato = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).virkningstidspunkt.atDay(1)

            val avkortingsinfo =
                Avkortingsinfo(
                    virkningsdato = virkningsdato,
                    beregningsperioder = avkorting.perioder.map { it.toAvkortetBeregningsperiode() },
                    endringIUtbetalingVedVirk = avkorting.endringIUtbetalingVedVirk,
                    erInnvilgelsesaar = avkorting.erInnvilgelsesaar,
                )

            val avkortetBeregningsperioder = avkortingsinfo.beregningsperioder
            val beregningsperioder = avkortetBeregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }

            val etterbetaling = behandlingInfoService.hentEtterbetaling(behandlingId)?.toEtterbetalingDTO()
            val trygdetid = trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo)
            krev(trygdetid.isNotEmpty()) { "Klarte ikke hente trygdetid for å generere vedtaksbrev" }

            val erTidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true

            val omsRettUtenTidsbegrensning =
                vilkaarsvurdering.vilkaar.single {
                    it.hovedvilkaar.type in
                        listOf(
                            VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                        )
                }

            val avdoede = grunnlag.mapAvdoede()

            val doedsdatoEllerOpphoertPleieforhold =
                if (erTidligereFamiliepleier) {
                    behandling.tidligereFamiliepleier!!.opphoertPleieforhold!!
                } else {
                    avdoede.single().doedsdato
                }
            val klage = hentKlageForBehandling(behandling)

            BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(null),
                avdoede = grunnlag.mapAvdoede(),
                verge = hentVergeForSak(sak.sakType, null, grunnlag),
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident(),
                attestantIdent = vedtak.attestasjon?.attestant ?: brukerTokenInfo.ident(),
                skalLagre = skalLagres,
                brevFastInnholdData =
                    OmstillingsstoenadInnvilgelseVedtakBrevData.Vedtak(
                        beregning =
                            omsBeregning(
                                behandling = behandling,
                                trygdetid = trygdetid.single(),
                                avkortingsinfo = avkortingsinfo,
                                landKodeverk = alleLand.await(),
                            ),
                        innvilgetMindreEnnFireMndEtterDoedsfall =
                            innvilgetMindreEnnFireMndEtterDoedsfall(
                                doedsdatoEllerOpphoertPleieforhold = doedsdatoEllerOpphoertPleieforhold,
                            ),
                        omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                        harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                        bosattUtland = behandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                        etterbetaling =
                            etterbetaling
                                ?.let { dto ->
                                    Etterbetaling.fraOmstillingsstoenadBeregningsperioder(
                                        dto,
                                        beregningsperioder,
                                    )
                                },
                        erSluttbehandling = behandling.erSluttbehandling,
                        tidligereFamiliepleier = erTidligereFamiliepleier,
                        datoVedtakOmgjoering = klage?.datoVedtakOmgjoering(),
                    ),
                brevRedigerbarInnholdData =
                    OmstillingsstoenadInnvilgelseVedtakBrevData.VedtakInnhold(
                        virkningsdato = avkortingsinfo.virkningsdato,
                        utbetalingsbeloep =
                            avkortingsinfo.beregningsperioder.firstOrNull()?.utbetaltBeloep
                                ?: throw UgyldigForespoerselException(
                                    "MANGLER_BEREGNINGSPERIODER_AVKORTING",
                                    "Mangler beregningsperioder i avkorting",
                                ),
                        etterbetaling = etterbetaling != null,
                        tidligereFamiliepleier = erTidligereFamiliepleier,
                        datoVedtakOmgjoering = klage?.datoVedtakOmgjoering(),
                        avdoed = if (erTidligereFamiliepleier) null else avdoede.single(),
                        erSluttbehandling = behandling.erSluttbehandling,
                        harUtbetaling = avkortingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                        beregning =
                            omsBeregning(
                                behandling = behandling,
                                trygdetid = trygdetid.single(),
                                avkortingsinfo = avkortingsinfo,
                                landKodeverk = emptyList(),
                            ),
                    ),
                brevVedleggData =
                    listOf(
                        OmstillingsstoenadInnvilgelseVedtakBrevData.beregningsvedleggInnhold(),
                    ),
            )
        }
    }

    private suspend fun utledBrevRequestRevurdering(
        brukerTokenInfo: BrukerTokenInfo,
        behandling: DetaljertBehandling,
        vedtak: VedtakDto,
        skalLagres: Boolean = false,
    ): BrevRequest {
        val behandlingId = behandling.id
        return coroutineScope {
            val virkningsdato = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).virkningstidspunkt.atDay(1)
            val avkorting = beregningKlient.hentBeregningOgAvkorting(behandlingId, brukerTokenInfo)
            val avkortingsinfo =
                Avkortingsinfo(
                    virkningsdato = virkningsdato,
                    beregningsperioder = avkorting.perioder.map { it.toAvkortetBeregningsperiode() },
                    endringIUtbetalingVedVirk = avkorting.endringIUtbetalingVedVirk,
                    erInnvilgelsesaar = avkorting.erInnvilgelsesaar,
                )

            val alleLand = async { kodeverkService.hentAlleLand(brukerTokenInfo) }
            val trygdetid = trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo)
            krev(trygdetid.isNotEmpty()) { "Klarte ikke hente trygdetid for å generere vedtaksbrev" }

            val brevutfall = async { behandlingInfoService.hentBrevutfall(behandlingId) }
            val vilkaarsvurdering = async { vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId) }

            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }
            val feilutbetaling =
                krevIkkeNull(
                    (
                        brevutfall.await()
                            ?: throw ManglerBrevutfall(behandlingId)
                    ).feilutbetaling?.valg?.let(FeilutbetalingType::fromFeilutbetalingValg),
                ) {
                    "Feilutbetaling mangler i brevutfall"
                }
            val sak = krevIkkeNull(sakService.finnSak(behandling.sak)) { "Sak må finnes" }
            val klage = hentKlageForBehandling(behandling)

            val beregningsperioderOpphoer =
                utledBeregningsperioderOpphoer(
                    behandling,
                    beregningsperioder,
                )
            val etterbetaling = behandlingInfoService.hentEtterbetaling(behandlingId)?.toEtterbetalingDTO()
            val sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode
            val omsRettUtenTidsbegrensning =
                krevIkkeNull(vilkaarsvurdering.await()) { "Mangler vilkarsvurdering" }.vilkaar.single {
                    it.hovedvilkaar.type in
                        listOf(
                            VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                        )
                }
            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlag(behandling.id)
                    ?: throw InternfeilException("Fant ikke grunnlag for behandlingId=${behandling.id}")
            val avdoede = grunnlag.mapAvdoede()

            BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(null),
                avdoede = grunnlag.mapAvdoede(),
                verge = hentVergeForSak(sak.sakType, null, grunnlag),
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident(),
                attestantIdent = vedtak.attestasjon?.attestant ?: brukerTokenInfo.ident(),
                skalLagre = skalLagres,
                brevFastInnholdData =
                    OmstillingsstoenadRevurderingVedtakBrevData.Vedtak(
                        erEndret =
                            avkortingsinfo.endringIUtbetalingVedVirk ||
                                behandling.revurderingsaarsak == Revurderingaarsak.FRA_0UTBETALING_TIL_UTBETALING,
                        erOmgjoering = behandling.revurderingsaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                        datoVedtakOmgjoering = klage?.datoVedtakOmgjoering(),
                        beregning =
                            OmstillingsstoenadBeregning(
                                virkningsdato = avkortingsinfo.virkningsdato,
                                beregningsperioder = beregningsperioder,
                                sisteBeregningsperiode = sisteBeregningsperiode,
                                sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
                                trygdetid =
                                    trygdetid.single().fromDto(
                                        beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                        beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                                        navnAvdoed = null,
                                        landKodeverk = alleLand.await(),
                                    ),
                                oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                                opphoerNesteAar =
                                    beregningsperioderOpphoer.forventetOpphoerDato?.year ==
                                        (behandling.virkningstidspunkt().dato.year + 1),
                                erYrkesskade = trygdetid.single().erYrkesskade(),
                            ),
                        omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                        feilutbetaling = feilutbetaling,
                        bosattUtland = behandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                        erInnvilgelsesaar = avkortingsinfo.erInnvilgelsesaar,
                        tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true,
                    ),
                brevRedigerbarInnholdData =
                    OmstillingsstoenadRevurderingVedtakBrevData.VedtakInnhold(
                        virkningsdato = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).virkningstidspunkt.atDay(1),
                        utbetalingsbeloep =
                            avkortingsinfo.beregningsperioder.firstOrNull()?.utbetaltBeloep
                                ?: throw UgyldigForespoerselException(
                                    "MANGLER_BEREGNINGSPERIODER_AVKORTING",
                                    "Mangler beregningsperioder i avkorting",
                                ),
                        etterbetaling = etterbetaling != null,
                        avdoed = if (behandling.tidligereFamiliepleier?.svar ?: false) null else avdoede.single(),
                        harUtbetaling = avkortingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                        beregning =
                            OmstillingsstoenadBeregning(
                                virkningsdato = avkortingsinfo.virkningsdato,
                                beregningsperioder = beregningsperioder,
                                sisteBeregningsperiode = sisteBeregningsperiode,
                                sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
                                trygdetid =
                                    trygdetid.single().fromDto(
                                        beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                                        beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                                        navnAvdoed = null,
                                        landKodeverk = alleLand.await(),
                                    ),
                                oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                                opphoerNesteAar =
                                    beregningsperioderOpphoer.forventetOpphoerDato?.year ==
                                        (behandling.virkningstidspunkt().dato.year + 1),
                                erYrkesskade = trygdetid.single().erYrkesskade(),
                            ),
                        erSluttbehandling = behandling.erSluttbehandling,
                        tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true,
                        datoVedtakOmgjoering = klage?.datoVedtakOmgjoering(),
                    ),
                brevVedleggData =
                    listOf(
                        OmstillingsstoenadInnvilgelseVedtakBrevData.beregningsvedleggInnhold(),
                    ),
            )
        }
    }

    fun hentKlageForBehandling(behandling: DetaljertBehandling): Klage? {
        validerRevurderingOmgjoringHarRelatertBehandlingId(behandling)
        val relatertBehandlingId = behandling.relatertBehandlingId ?: return null
        if (behandling.kanHaKlage()) {
            return klageService.hentKlage(relatertBehandlingId) // returnerer null hvis vi ikke finner klage
        }
        return null
    }
}

private fun validerRevurderingOmgjoringHarRelatertBehandlingId(behandling: DetaljertBehandling) {
    if (behandling.revurderingsaarsak == OMGJOERING_ETTER_KLAGE && behandling.relatertBehandlingId == null) {
        throw InternfeilException("Behandling med revurderingsårsak OMGJOERING_ETTER_KLAGE må ha relatertBehandlingId")
    }
}

/*
┌───────────────────────────────────────────────────────┬──────────────────────────────────┐
│ Behandlingstype + årsak                               │ relatertBehandlingId peker på    │
├───────────────────────────────────────────────────────┼──────────────────────────────────┤
│ FØRSTEGANGSBEHANDLING (omgjøring av avslag pga klage) │ klage-UUID (eller null)          │
├───────────────────────────────────────────────────────┼──────────────────────────────────┤
│ REVURDERING – OMGJOERING_ETTER_KLAGE                  │ alltid klage-UUID                │
├───────────────────────────────────────────────────────┼──────────────────────────────────┤
│ REVURDERING – ETTEROPPGJOER                           │ forbehandling-UUID (ikke klage!) │
└───────────────────────────────────────────────────────┴──────────────────────────────────┘
*/
private fun DetaljertBehandling.kanHaKlage(): Boolean =
    this.revurderingsaarsak == OMGJOERING_ETTER_KLAGE || this.behandlingType == FØRSTEGANGSBEHANDLING

fun innvilgetMindreEnnFireMndEtterDoedsfall(
    innvilgelsesDato: LocalDate = LocalDate.now(),
    doedsdatoEllerOpphoertPleieforhold: LocalDate,
): Boolean = innvilgelsesDato.isBefore(doedsdatoEllerOpphoertPleieforhold.plusMonths(4))
