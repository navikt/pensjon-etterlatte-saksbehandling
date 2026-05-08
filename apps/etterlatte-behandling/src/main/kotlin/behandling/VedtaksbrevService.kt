package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.aktivitetsplikt.ManglerBehandling
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoService
import no.nav.etterlatte.behandling.klage.KlageService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakInternalService
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentVergeForSak
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.oms.FeilutbetalingType
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
import no.nav.etterlatte.libs.common.behandling.FeilutbetalingValg
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak.OMGJOERING_ETTER_KLAGE
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
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
    suspend fun opprettBrev(
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
    ): BrevRequest {
        val detaljertBehandling =
            behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo)
                ?: throw ManglerBehandling("")

        return when (detaljertBehandling.behandlingType) {
            FØRSTEGANGSBEHANDLING -> {
                utledBrevRequestInnvilgelse(detaljertBehandling, brukerTokenInfo, behandlingId, skalLagres)
            }

            REVURDERING -> {
                utledBrevRequestRevurdering(detaljertBehandling, brukerTokenInfo, behandlingId, skalLagres)
            }
        }
    }

    private suspend fun utledBrevRequestInnvilgelse(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
        skalLagres: Boolean = false,
    ): BrevRequest =
        coroutineScope {
            val vedtakDeferred = async { vedtakInternalService.hentVedtak(behandlingId, brukerTokenInfo) }
            val alleLand = async { kodeverkService.hentAlleLand(brukerTokenInfo) }

            val vedtak =
                vedtakDeferred.await()
                    ?: throw InternfeilException("Fant ikke vedtak for behandlingId=$behandlingId")

            val sak = krevIkkeNull(sakService.finnSak(behandling.sak)) { "Sak må finnes" }

            val vilkaarsvurdering =
                krevIkkeNull(vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)) { "Må ha vilkårsvurdering" }

            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlag(behandling.id)
                    ?: throw InternfeilException("Fant ikke grunnlag for behandlingId=${behandling.id}")

            val virkningsdato = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).virkningstidspunkt.atDay(1)

            val avkortingsinfo = hentAvkortingsinfo(behandlingId, brukerTokenInfo, virkningsdato)

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

    private suspend fun hentAvkortingsinfo(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        virkningsdato: LocalDate,
    ): Avkortingsinfo {
        val avkorting = beregningKlient.hentBeregningOgAvkorting(behandlingId, brukerTokenInfo)
        val avkortingsinfo =
            Avkortingsinfo(
                virkningsdato = virkningsdato,
                beregningsperioder = avkorting.perioder.map { it.toAvkortetBeregningsperiode() },
                endringIUtbetalingVedVirk = avkorting.endringIUtbetalingVedVirk,
                erInnvilgelsesaar = avkorting.erInnvilgelsesaar,
            )
        return avkortingsinfo
    }

    private suspend fun utledBrevRequestRevurdering(
        behandling: DetaljertBehandling,
        bruker: BrukerTokenInfo,
        behandlingId: UUID,
        skalLagres: Boolean,
        klage: Klage? = null, // TODO
        revurderingaarsak: Revurderingaarsak? = null, // TODO
        utlandstilknytningType: UtlandstilknytningType? = null, // TODO
    ): BrevRequest =
        coroutineScope {
            val vedtakDeferred = async { vedtakInternalService.hentVedtak(behandlingId, bruker) }
            val vedtak =
                vedtakDeferred.await()
                    ?: throw InternfeilException("Fant ikke vedtak for behandlingId=$behandlingId")
            val virkningsdato = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).virkningstidspunkt.atDay(1)
            val avkortingsinfo = hentAvkortingsinfo(behandlingId, bruker, virkningsdato)
            val trygdetid = trygdetidKlient.hentTrygdetid(behandlingId, bruker)
            krev(trygdetid.isNotEmpty()) { "Klarte ikke hente trygdetid for å generere vedtaksbrev" }

            val brevutfall = behandlingInfoService.hentBrevutfall(behandlingId)
            val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)

            val land = async { kodeverkService.hentAlleLand(bruker) }

            val beregningsperioder =
                avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }
            val feilutbetaling =
                krevIkkeNull(
                    (
                        brevutfall
                            ?: throw ManglerBrevutfall(behandlingId)
                    ).feilutbetaling?.valg?.let(::toFeilutbetalingType),
                ) {
                    "Feilutbetaling mangler i brevutfall"
                }
            val beregningsperioderOpphoer =
                utledBeregningsperioderOpphoer(
                    behandling,
                    beregningsperioder,
                )
            val sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode
            val omsRettUtenTidsbegrensning =
                krevIkkeNull(vilkaarsvurdering) { "Mangler vilkarsvurdering" }.vilkaar.single {
                    it.hovedvilkaar.type in
                        listOf(
                            VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING,
                        )
                }

            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlag(behandling.id)
                    ?: throw InternfeilException("Fant ikke grunnlag for behandlingId=${behandling.id}")
            val sak = krevIkkeNull(sakService.finnSak(behandling.sak)) { "Sak må finnes" }

            BrevRequest(
                sak = sak,
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(null),
                avdoede = grunnlag.mapAvdoede(),
                verge = hentVergeForSak(sak.sakType, null, grunnlag),
                spraak = grunnlag.mapSpraak(),
                saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: bruker.ident(),
                attestantIdent = vedtak.attestasjon?.attestant ?: bruker.ident(),
                skalLagre = skalLagres,
                brevFastInnholdData =
                    OmstillingsstoenadRevurderingVedtakBrevData.Vedtak(
                        // innholdForhaandsvarsel = emptyList()//TODO
//                vedleggHvisFeilutbetaling(
//                    feilutbetaling,
//                    innholdMedVedlegg,
//                    BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING,
//                ),
                        erEndret =
                            avkortingsinfo.endringIUtbetalingVedVirk ||
                                revurderingaarsak == Revurderingaarsak.FRA_0UTBETALING_TIL_UTBETALING,
                        erOmgjoering = revurderingaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                        datoVedtakOmgjoering = klage?.datoVedtakOmgjoering(),
                        beregning =
                            omsBeregning(
                                behandling = behandling,
                                trygdetid = trygdetid.single(),
                                avkortingsinfo = avkortingsinfo,
                                landKodeverk = land.await(),
                            ),
                        omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning.hovedvilkaar.resultat == Utfall.OPPFYLT,
                        feilutbetaling = feilutbetaling,
                        bosattUtland = utlandstilknytningType == UtlandstilknytningType.BOSATT_UTLAND,
                        erInnvilgelsesaar = avkortingsinfo.erInnvilgelsesaar,
                        tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true,
                    ),
                brevRedigerbarInnholdData = null, // TODO
                brevVedleggData = emptyList(),
            ) // TODO
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

class ManglerBrevutfall( // TODO get from other module?
    behandlingId: UUID?,
) : UgyldigForespoerselException(
        code = "BEHANDLING_MANGLER_BREVUTFALL",
        detail = "Behandling mangler brevutfall, som er påkrevd. Legg til dette ved å lagre Valg av utfall i brev.",
        meta = mapOf("behandlingId" to behandlingId.toString()),
    )

fun toFeilutbetalingType(feilutbetalingValg: FeilutbetalingValg) =
    when (feilutbetalingValg) {
        FeilutbetalingValg.NEI -> FeilutbetalingType.INGEN_FEILUTBETALING
        FeilutbetalingValg.JA_INGEN_TK -> FeilutbetalingType.FEILUTBETALING_4RG_UTEN_VARSEL
        FeilutbetalingValg.JA_INGEN_VARSEL_MOTREGNES -> FeilutbetalingType.FEILUTBETALING_UTEN_VARSEL
        FeilutbetalingValg.JA_VARSEL -> FeilutbetalingType.FEILUTBETALING_MED_VARSEL
    }

// fun vedleggHvisFeilutbetaling(
//    feilutbetaling: FeilutbetalingType,
//    innholdMedVedlegg: InnholdMedVedlegg,
//    brevVedleggKey: BrevVedleggKey,
// ) = if (feilutbetaling == FeilutbetalingType.FEILUTBETALING_MED_VARSEL) {
//    innholdMedVedlegg.finnVedlegg(brevVedleggKey)
// } else {
//    emptyList()
// }
