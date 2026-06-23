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
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.BrevVedleggRedigerbarNy
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.ManglerBrevutfall
import no.nav.etterlatte.brev.Vedlegg
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Etterbetaling
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.erYrkesskade
import no.nav.etterlatte.brev.model.fromDto
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadBeregningRedigerbartUtfall
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadBeregningRedigerbartVedleggData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseVedtakBrevData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurderingVedtakBrevData
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadRevurderingVedtakBrevData.VedtakInnhold
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
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak.AARLIG_INNTEKTSJUSTERING
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak.FRA_0UTBETALING_TIL_UTBETALING
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak.INNTEKTSENDRING
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
            } else if (vedtak.type == VedtakType.OPPHOER) {
                utledBrevRequestOpphoer(brukerTokenInfo, behandling, vedtak, skalLagres)
            } else if (behandling.revurderingsaarsak == AARLIG_INNTEKTSJUSTERING) {
                utledBrevRequestAarligInntektsjustering(brukerTokenInfo, behandling, vedtak, skalLagres)
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
        val fellesData = hentFellesData(behandling, vedtak, brukerTokenInfo)
        val erTidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true
        val doedsdatoEllerOpphoertPleieforhold =
            if (erTidligereFamiliepleier) {
                behandling.tidligereFamiliepleier!!.opphoertPleieforhold!!
            } else {
                fellesData.avdoede.single().doedsdato
            }

        return BrevRequest(
            sak = fellesData.sak,
            innsender = fellesData.innsender(),
            soeker = fellesData.soeker(),
            avdoede = fellesData.avdoede,
            verge = fellesData.verge(),
            spraak = fellesData.spraak(),
            saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident(),
            attestantIdent = vedtak.attestasjon?.attestant ?: brukerTokenInfo.ident(),
            skalLagre = skalLagres,
            brevFastInnholdData =
                OmstillingsstoenadInnvilgelseVedtakBrevData.Vedtak(
                    beregning =
                        omsBeregning(
                            behandling = behandling,
                            trygdetid = fellesData.trygdetid.single(),
                            avkortingsinfo = fellesData.avkortingsinfo,
                            landKodeverk = fellesData.alleLand,
                        ),
                    innvilgetMindreEnnFireMndEtterDoedsfall =
                        innvilgetMindreEnnFireMndEtterDoedsfall(
                            doedsdatoEllerOpphoertPleieforhold = doedsdatoEllerOpphoertPleieforhold,
                        ),
                    omsRettUtenTidsbegrensning = fellesData.omsRettUtenTidsbegrensning,
                    harUtbetaling = fellesData.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                    bosattUtland = behandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                    etterbetaling =
                        fellesData.etterbetaling?.let { dto ->
                            Etterbetaling.fraOmstillingsstoenadBeregningsperioder(dto, fellesData.beregningsperioder)
                        },
                    erSluttbehandling = behandling.erSluttbehandling,
                    tidligereFamiliepleier = erTidligereFamiliepleier,
                    datoVedtakOmgjoering = fellesData.klage?.datoVedtakOmgjoering(),
                ),
            brevRedigerbarInnholdData =
                OmstillingsstoenadInnvilgelseVedtakBrevData.VedtakInnhold(
                    virkningsdato = fellesData.avkortingsinfo.virkningsdato,
                    utbetalingsbeloep =
                        fellesData.avkortingsinfo.beregningsperioder
                            .firstOrNull()
                            ?.utbetaltBeloep
                            ?: throw UgyldigForespoerselException(
                                "MANGLER_BEREGNINGSPERIODER_AVKORTING",
                                "Mangler beregningsperioder i avkorting",
                            ),
                    etterbetaling = fellesData.etterbetaling != null,
                    tidligereFamiliepleier = erTidligereFamiliepleier,
                    datoVedtakOmgjoering = fellesData.klage?.datoVedtakOmgjoering(),
                    avdoed = if (erTidligereFamiliepleier) null else fellesData.avdoede.single(),
                    erSluttbehandling = behandling.erSluttbehandling,
                    harUtbetaling = fellesData.harUtbetaling(),
                    beregning =
                        omsBeregning(
                            behandling = behandling,
                            trygdetid = fellesData.trygdetid.single(),
                            avkortingsinfo = fellesData.avkortingsinfo,
                            landKodeverk = emptyList(),
                        ),
                ),
            brevVedleggData =
                listOf(
                    OmstillingsstoenadInnvilgelseVedtakBrevData.beregningsvedleggInnhold(),
                ),
        )
    }

    private suspend fun utledBrevRequestRevurdering(
        brukerTokenInfo: BrukerTokenInfo,
        behandling: DetaljertBehandling,
        vedtak: VedtakDto,
        skalLagres: Boolean = false,
    ): BrevRequest {
        val fellesData = hentFellesData(behandling, vedtak, brukerTokenInfo)

        val feilutbetaling =
            krevIkkeNull(
                fellesData.brevutfall.feilutbetaling
                    ?.valg
                    ?.let(FeilutbetalingType::fromFeilutbetalingValg),
            ) { "Feilutbetaling mangler i brevutfall" }

        val beregningsperioderOpphoer = utledBeregningsperioderOpphoer(behandling, fellesData.beregningsperioder)
        val sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode
        val beregning =
            OmstillingsstoenadBeregning(
                virkningsdato = fellesData.avkortingsinfo.virkningsdato,
                beregningsperioder = fellesData.beregningsperioder,
                sisteBeregningsperiode = sisteBeregningsperiode,
                sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
                trygdetid =
                    fellesData.trygdetid.single().fromDto(
                        beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                        beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                        navnAvdoed = null,
                        landKodeverk = fellesData.alleLand,
                    ),
                oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                opphoerNesteAar =
                    beregningsperioderOpphoer.forventetOpphoerDato?.year ==
                        (behandling.virkningstidspunkt().dato.year + 1),
                erYrkesskade = fellesData.trygdetid.single().erYrkesskade(),
            )

        return BrevRequest(
            sak = fellesData.sak,
            innsender = fellesData.innsender(),
            soeker = fellesData.soeker(),
            avdoede = fellesData.avdoede,
            verge = fellesData.verge(),
            spraak = fellesData.spraak(),
            saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident(),
            attestantIdent = vedtak.attestasjon?.attestant ?: brukerTokenInfo.ident(),
            skalLagre = skalLagres,
            brevFastInnholdData =
                OmstillingsstoenadRevurderingVedtakBrevData.Vedtak(
                    erEndret =
                        fellesData.avkortingsinfo.endringIUtbetalingVedVirk ||
                            behandling.revurderingsaarsak == FRA_0UTBETALING_TIL_UTBETALING,
                    erOmgjoering = behandling.revurderingsaarsak == OMGJOERING_ETTER_KLAGE,
                    datoVedtakOmgjoering = fellesData.klage?.datoVedtakOmgjoering(),
                    beregning = beregning,
                    omsRettUtenTidsbegrensning = fellesData.omsRettUtenTidsbegrensning,
                    feilutbetaling = feilutbetaling,
                    bosattUtland = behandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                    erInnvilgelsesaar = fellesData.avkortingsinfo.erInnvilgelsesaar,
                    tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true,
                ),
            brevRedigerbarInnholdData =
                VedtakInnhold(
                    beregning =
                        OmstillingsstoenadBeregningRedigerbartUtfall(
                            virkningsdato = fellesData.avkortingsinfo.virkningsdato,
                            beregningsperioder = fellesData.beregningsperioder,
                            sisteBeregningsperiode = sisteBeregningsperiode,
                            sisteBeregningsperiodeNesteAar = beregningsperioderOpphoer.sisteBeregningsperiodeNesteAar,
                            oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                            opphoerNesteAar =
                                beregningsperioderOpphoer.forventetOpphoerDato?.year == (behandling.virkningstidspunkt().dato.year + 1),
                        ),
                    erEndret =
                        fellesData.avkortingsinfo.endringIUtbetalingVedVirk ||
                            behandling.revurderingsaarsak == FRA_0UTBETALING_TIL_UTBETALING,
                    erEtterbetaling = fellesData.etterbetaling != null,
                    etterbetaling =
                        fellesData.etterbetaling?.let {
                            Etterbetaling.fraOmstillingsstoenadBeregningsperioder(
                                it,
                                fellesData.beregningsperioder,
                            )
                        },
                    feilutbetaling =
                        krevIkkeNull(
                            fellesData.brevutfall.feilutbetaling
                                ?.valg
                                ?.let(::toFeilutbetalingType),
                        ) {
                            "Feilutbetaling mangler i brevutfall"
                        },
                    harFlereUtbetalingsperioder = fellesData.beregningsperioder.size > 1,
                    harUtbetaling = fellesData.beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                    inntekt = sisteBeregningsperiode.inntekt,
                    inntektsAar = sisteBeregningsperiode.datoFOM.year,
                    mottattInntektendringAutomatisk =
                        if (behandling.prosesstype == Prosesstype.AUTOMATISK &&
                            behandling.revurderingsaarsak == INNTEKTSENDRING
                        ) {
                            behandling.mottattDato?.toLocalDate()
                                ?: throw InternfeilException("Automatisk inntektsendring må ha mottatt dato")
                        } else {
                            null
                        },
                ),
            brevVedleggData =
                listOf(
                    BrevVedleggRedigerbarNy(
                        data =
                            OmstillingsstoenadBeregningRedigerbartVedleggData(
                                omstillingsstoenadBeregning = beregning,
                                erInnvilgelsesAar = fellesData.avkortingsinfo.erInnvilgelsesaar,
                            ),
                        vedlegg = Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                        vedleggId = BrevVedleggKey.OMS_BEREGNING,
                    ),
                ),
        )
    }

    private suspend fun utledBrevRequestAarligInntektsjustering(
        brukerTokenInfo: BrukerTokenInfo,
        behandling: DetaljertBehandling,
        vedtak: VedtakDto,
        skalLagres: Boolean = false,
    ): BrevRequest {
        val fellesData = hentFellesData(behandling, vedtak, brukerTokenInfo)

        val trygdetid = krevIkkeNull(fellesData.trygdetid) { "Mangler trygdetid" }.single()
        val beregningsperioder =
            fellesData.avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }
        val beregningsperioderOpphoer =
            utledBeregningsperioderOpphoer(
                behandling = behandling,
                beregningsperioder = beregningsperioder,
            )
        val sisteBeregningsperiode = beregningsperioderOpphoer.sisteBeregningsperiode
        val virk = behandling.virkningstidspunkt!!.dato.atDay(1)
        val omsRettUtenTidsbegrensning = fellesData.omsRettUtenTidsbegrensning

        val beregning =
            OmstillingsstoenadBeregning(
                virkningsdato = virk,
                beregningsperioder = beregningsperioder,
                sisteBeregningsperiode = sisteBeregningsperiode,
                sisteBeregningsperiodeNesteAar = null,
                trygdetid =
                    trygdetid.fromDto(
                        beregningsMetodeFraGrunnlag = sisteBeregningsperiode.beregningsMetodeFraGrunnlag,
                        beregningsMetodeAnvendt = sisteBeregningsperiode.beregningsMetodeAnvendt,
                        navnAvdoed = null,
                        landKodeverk = fellesData.alleLand,
                    ),
                oppphoersdato = beregningsperioderOpphoer.forventetOpphoerDato,
                opphoerNesteAar =
                    beregningsperioderOpphoer.forventetOpphoerDato?.year == (behandling.virkningstidspunkt().dato.year + 1),
                erYrkesskade = trygdetid.erYrkesskade(),
            )
        return BrevRequest(
            sak = fellesData.sak,
            innsender = fellesData.innsender(),
            soeker = fellesData.soeker(),
            avdoede = fellesData.avdoede,
            verge = fellesData.verge(),
            spraak = fellesData.spraak(),
            saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident(),
            attestantIdent = vedtak.attestasjon?.attestant ?: brukerTokenInfo.ident(),
            skalLagre = skalLagres,
            brevFastInnholdData =
                OmstillingsstoenadRevurderingVedtakBrevData.VedtakAarligInntektsjustering(
                    beregning =
                    beregning,
                    omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning,
                    tidligereFamiliepleier = behandling.tidligereFamiliepleier?.svar == true,
                    inntektsaar = virk.year,
                    harUtbetaling = beregningsperioder.any { it.utbetaltBeloep.value > 0 },
                    endringIUtbetaling = fellesData.avkortingsinfo.endringIUtbetalingVedVirk,
                    virkningstidspunkt = virk,
                    bosattUtland = behandling.erBosattUtland(),
                ),
            brevRedigerbarInnholdData =
                OmstillingsstoenadRevurderingVedtakBrevData.VedtakInnholdAarligInntektsjustering(
                    inntektsbeloep = sisteBeregningsperiode.inntekt,
                    inntektsaar = virk.year,
                ),
            brevVedleggData =
                listOf(
                    BrevVedleggRedigerbarNy(
                        data =
                            OmstillingsstoenadBeregningRedigerbartVedleggData(
                                omstillingsstoenadBeregning = beregning,
                                erInnvilgelsesAar = fellesData.avkortingsinfo.erInnvilgelsesaar,
                            ),
                        vedlegg = Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL,
                        vedleggId = BrevVedleggKey.OMS_BEREGNING,
                    ),
                ),
        )
    }

    private suspend fun utledBrevRequestOpphoer(
        brukerTokenInfo: BrukerTokenInfo,
        behandling: DetaljertBehandling,
        vedtak: VedtakDto,
        skalLagres: Boolean = false,
    ): BrevRequest {
        val fellesData = hentFellesData(behandling, vedtak, brukerTokenInfo)
        return BrevRequest(
            sak = fellesData.sak,
            innsender = fellesData.innsender(),
            soeker = fellesData.soeker(),
            avdoede = fellesData.avdoede,
            verge = fellesData.verge(),
            spraak = fellesData.spraak(),
            saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: brukerTokenInfo.ident(),
            attestantIdent = vedtak.attestasjon?.attestant ?: brukerTokenInfo.ident(),
            skalLagre = skalLagres,
            brevFastInnholdData =
                OmstillingsstoenadRevurderingVedtakBrevData.VedtakOpphoer(
                    bosattUtland = behandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                    virkningsdato = fellesData.virkningsdato,
                    feilutbetaling = fellesData.feilutbetalingFraBrevutfall(),
                ),
            brevRedigerbarInnholdData =
                OmstillingsstoenadRevurderingVedtakBrevData.VedtakInnholdOpphoer(
                    feilutbetaling = fellesData.feilutbetalingFraBrevutfall(),
                ),
            brevVedleggData = emptyList(),
        )
    }

    private suspend fun hentFellesData(
        behandling: DetaljertBehandling,
        vedtak: VedtakDto,
        brukerTokenInfo: BrukerTokenInfo,
    ): OmsVedtaksbrevGrunnlag =
        coroutineScope {
            val behandlingId = behandling.id
            val virkningsdato = (vedtak.innhold as VedtakInnholdDto.VedtakBehandlingDto).virkningstidspunkt.atDay(1)

            val alleLand = async { kodeverkService.hentAlleLand(brukerTokenInfo) }
            val trygdetid = async { trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo) }

            val avkorting = beregningKlient.hentBeregningOgAvkorting(behandlingId, brukerTokenInfo)
            val avkortingsinfo =
                Avkortingsinfo(
                    virkningsdato = virkningsdato,
                    beregningsperioder = avkorting.perioder.map { it.toAvkortetBeregningsperiode() },
                    endringIUtbetalingVedVirk = avkorting.endringIUtbetalingVedVirk,
                    erInnvilgelsesaar = avkorting.erInnvilgelsesaar,
                )
            val beregningsperioder = avkortingsinfo.beregningsperioder.map { it.tilOmstillingsstoenadBeregningsperiode() }

            val sak = krevIkkeNull(sakService.finnSak(behandling.sak)) { "Sak må finnes" }
            val grunnlag =
                grunnlagService.hentOpplysningsgrunnlag(behandling.id)
                    ?: throw InternfeilException("Fant ikke grunnlag for behandlingId=${behandling.id}")
            val avdoede = grunnlag.mapAvdoede()
            val klage = hentKlageForBehandling(behandling)
            val etterbetaling = behandlingInfoService.hentEtterbetaling(behandlingId)?.toEtterbetalingDTO()

            val trygdetidAwait = trygdetid.await()
            krev(trygdetidAwait.isNotEmpty()) { "Klarte ikke hente trygdetid for å generere vedtaksbrev" }

            val vilkaarsvurdering =
                krevIkkeNull(vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)) { "Mangler vilkårsvurdering" }
            val omsRettUtenTidsbegrensning =
                vilkaarsvurdering.vilkaar
                    .single { it.hovedvilkaar.type in listOf(VilkaarType.OMS_RETT_UTEN_TIDSBEGRENSNING) }
                    .let { it.hovedvilkaar.resultat == Utfall.OPPFYLT }

            val brevutfall = behandlingInfoService.hentBrevutfall(behandlingId) ?: throw ManglerBrevutfall(behandlingId)

            OmsVedtaksbrevGrunnlag(
                virkningsdato = virkningsdato,
                avkortingsinfo = avkortingsinfo,
                beregningsperioder = beregningsperioder,
                alleLand = alleLand.await(),
                trygdetid = trygdetidAwait,
                sak = sak,
                grunnlag = grunnlag,
                avdoede = avdoede,
                omsRettUtenTidsbegrensning = omsRettUtenTidsbegrensning,
                klage = klage,
                etterbetaling = etterbetaling,
                brevutfall = brevutfall,
            )
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

private fun OmsVedtaksbrevGrunnlag.harUtbetaling(): Boolean = avkortingsinfo.beregningsperioder.any { it.utbetaltBeloep.value > 0 }

private fun toFeilutbetalingType(feilutbetalingValg: FeilutbetalingValg) =
    when (feilutbetalingValg) {
        FeilutbetalingValg.NEI -> FeilutbetalingType.INGEN_FEILUTBETALING
        FeilutbetalingValg.JA_INGEN_TK -> FeilutbetalingType.FEILUTBETALING_4RG_UTEN_VARSEL
        FeilutbetalingValg.JA_INGEN_VARSEL_MOTREGNES -> FeilutbetalingType.FEILUTBETALING_UTEN_VARSEL
        FeilutbetalingValg.JA_VARSEL -> FeilutbetalingType.FEILUTBETALING_MED_VARSEL
    }
