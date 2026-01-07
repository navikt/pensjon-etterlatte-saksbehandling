package no.nav.etterlatte.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.behandling.behandlinginfo.BehandlingInfoService
import no.nav.etterlatte.behandling.klage.KlageService
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.mapAvdoede
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentVergeForSak
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.oms.OmstillingsstoenadInnvilgelseVedtakBrevData
import no.nav.etterlatte.brev.model.oms.omsBeregning
import no.nav.etterlatte.brev.model.oms.toAvkortetBeregningsperiode
import no.nav.etterlatte.brev.model.oms.utledBeregningsperioderOpphoer
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.vedtak.VedtakInnholdDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sak.SakService
import no.nav.etterlatte.vilkaarsvurdering.service.VilkaarsvurderingService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class VedtaksbrevService(
    private val grunnlagService: GrunnlagService,
    private val vedtakKlient: VedtakKlient,
    private val brevKlient: BrevKlient,
    private val behandlingService: BehandlingService,
    private val beregningKlient: BeregningKlient,
    private val brevApiKlient: BrevApiKlient,
    private val behandlingInfoService: BehandlingInfoService,
    private val trygdetidKlient: TrygdetidKlient,
    private val vilkaarsvurderingService: VilkaarsvurderingService,
    private val sakService: SakService,
    private val klageService: KlageService,
) {
    private val logger = LoggerFactory.getLogger(VedtaksbrevService::class.java)

    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Brev {
        val brevRequest = retryOgPakkUt { utledBrevRequest(bruker, behandlingId, sakId) }

        return brevKlient.opprettStrukturertBrev(
            behandlingId,
            brevRequest,
            bruker,
        )
    }

    private suspend fun utledBrevRequest(
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
        sakId: SakId,
        skalLagres: Boolean = false,
    ): BrevRequest =
        coroutineScope {
            val vedtakDeferred = async { vedtakKlient.hentVedtak(behandlingId, brukerTokenInfo) }

            val vedtak =
                vedtakDeferred.await()
                    ?: throw InternfeilException("Fant ikke vedtak for behandlingId=$behandlingId")

            val behandling =
                behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo)
                    ?: throw InternfeilException("Fant ikke behandlingId=$behandlingId")
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
            val klage = hentKlageForBehandling(behandling.relatertBehandlingId, behandling)

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
                                vedleggInnhold = emptyList(), // TODO
                                behandling = behandling,
                                trygdetid = trygdetid.single(),
                                avkortingsinfo = avkortingsinfo,
                                landKodeverk = emptyList(),
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
                                    no.nav.etterlatte.brev.model.Etterbetaling.fraOmstillingsstoenadBeregningsperioder(
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
                                vedleggInnhold = emptyList(), // TODO
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

    fun hentKlageForBehandling(
        relatertBehandlingId: String?,
        behandling: DetaljertBehandling?,
    ): Klage? =
        if (behandling?.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING && relatertBehandlingId != null) {
            try {
                val klageId = UUID.fromString(relatertBehandlingId)
                klageService.hentKlage(klageId)
            } catch (e: Exception) {
                logger.error("Fant ikke klage med id=$relatertBehandlingId", e)
                logger.info(
                    "Kunne ikke finne klage med id=$relatertBehandlingId, denne førstegangsbehandlingen med id=${behandling.id} gjelder ikke omgjøring på grunn av klage",
                )
                null
            }
        } else if (behandling?.revurderingsaarsak == Revurderingaarsak.OMGJOERING_ETTER_KLAGE) {
            val klageId = UUID.fromString(relatertBehandlingId)
            klageService.hentKlage(klageId)
        } else {
            null
        }
}

fun innvilgetMindreEnnFireMndEtterDoedsfall(
    innvilgelsesDato: LocalDate = LocalDate.now(),
    doedsdatoEllerOpphoertPleieforhold: LocalDate,
): Boolean = innvilgelsesDato.isBefore(doedsdatoEllerOpphoertPleieforhold.plusMonths(4))

// Brukes der mangler med trygdetid ikke skal kunne skje men felt likevel er nullable
class ManglerMedTrygdetidVeBrukIBrev :
    UgyldigForespoerselException(
        code = "MANGLER_TRYGDETID_VED_BREV",
        detail = "Trygdetid har mangler ved bruk til brev",
    )

class ManglerAvdoedBruktTilTrygdetid :
    UgyldigForespoerselException(
        code = "MANGLER_AVDOED_INFO_I_BEREGNING",
        detail = "Det mangler avdød i beregning. Utfør beregning på nytt og prøv igjen.",
    )

class FantIkkeIdentTilTrygdetidBlantAvdoede :
    UgyldigForespoerselException(
        code = "FANT_IKKE_TRYGDETID_IDENT_BLANT_AVDOEDE",
        detail = "Ident knyttet til trygdetid er ikke blant avdøde knyttet til sak",
    )

class OverstyrtTrygdetidManglerAvdoed :
    UgyldigForespoerselException(
        code = "OVERSTYRT_TRYGDETID_MANGLER_AVDOED",
        detail = "Overstyrt trygdetid mangler avdød. Trygdetiden må overskrives med ny overstyrt trygdetid",
    )

class ManglerFrivilligSkattetrekk(
    behandlingId: UUID?,
) : UgyldigForespoerselException(
        code = "BEHANDLING_MANGLER_FRIVILLIG_SKATTETREKK",
        detail =
            "Behandling mangler informasjon om frivillig skattetrekk, som er påkrevd for barnepensjon. " +
                "Du kan legge til dette i Valg av utfall i brev.",
        meta = mapOf("behandlingId" to behandlingId.toString()),
    )

class ManglerBrevutfall(
    behandlingId: UUID?,
) : UgyldigForespoerselException(
        code = "BEHANDLING_MANGLER_BREVUTFALL",
        detail = "Behandling mangler brevutfall, som er påkrevd. Legg til dette ved å lagre Valg av utfall i brev.",
        meta = mapOf("behandlingId" to behandlingId.toString()),
    )

private fun List<LandDto>.hentLandFraLandkode(landkode: String) = firstOrNull { it.isoLandkode == landkode }?.beskrivelse?.tekst

class UgyldigBeregningsMetode :
    UgyldigForespoerselException(
        code = "UGYLDIG_BEREGNINGS_METODE",
        detail =
            "Kan ikke ha brukt beregningsmetode 'BEST' i en faktisk beregning, " +
                "siden best velger mellom nasjonal eller prorata når det beregnes.",
    )

data class BeregningsperioderFlereAarOpphoer(
    val sisteBeregningsperiode: OmstillingsstoenadInnvilgelseVedtakBrevData.OmstillingsstoenadBeregningsperiode,
    val sisteBeregningsperiodeNesteAar: OmstillingsstoenadInnvilgelseVedtakBrevData.OmstillingsstoenadBeregningsperiode?,
    val forventetOpphoerDato: LocalDate?,
)
