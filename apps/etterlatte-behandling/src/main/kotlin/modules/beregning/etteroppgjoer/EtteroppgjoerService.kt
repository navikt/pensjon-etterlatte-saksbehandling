package no.nav.etterlatte.modules.beregning.etteroppgjoer

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakEtteroppgjoerService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransactionIfNeeded
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vedtak.VedtakEtteroppgjoerPeriode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.RegelPeriode
import no.nav.etterlatte.libs.regler.RegelkjoeringResultat
import no.nav.etterlatte.libs.regler.eksekver
import no.nav.etterlatte.modules.beregning.AvkortingRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.BeregningToggles
import no.nav.etterlatte.modules.beregning.EtteroppgjoerRepositoryOperasjoner
import no.nav.etterlatte.modules.beregning.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.modules.beregning.avkorting.Avkorting
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingReparerAarsoppgjoeret
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingService
import no.nav.etterlatte.modules.beregning.avkorting.Etteroppgjoer
import no.nav.etterlatte.modules.beregning.avkorting.regler.EtteroppgjoerDifferanseGrunnlag
import no.nav.etterlatte.modules.beregning.avkorting.regler.beregneEtteroppgjoerRegelMedDoedsfall
import no.nav.etterlatte.modules.beregning.avkorting.toDto
import no.nav.etterlatte.modules.beregning.sanksjon.SanksjonService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class EtteroppgjoerBehandlingerTilhoererIkkeSammeSak :
    UgyldigForespoerselException(
        code = "BEHANDLINGER_TILHOERER_IKKE_SAMME_SAK",
        detail = "Forbehandlingen, siste iverksatte behandling og angitt sak må tilhøre samme sak",
    )

class EtteroppgjoerService(
    private val avkortingRepository: AvkortingRepositoryOperasjoner,
    private val sanksjonService: SanksjonService,
    private val etteroppgjoerRepository: EtteroppgjoerRepositoryOperasjoner,
    private val avkortingService: AvkortingService,
    private val reparerAarsoppgjoeret: AvkortingReparerAarsoppgjoeret,
    private val vedtakEtteroppgjoerService: VedtakEtteroppgjoerService,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
    private val forbehandlingService: EtteroppgjoerForbehandlingService,
) {
    private val logger = LoggerFactory.getLogger(EtteroppgjoerService::class.java)

    fun beregnOgLagreEtteroppgjoer(
        request: EtteroppgjoerBeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultat {
        val avkorting = beregnAvkortingForbehandling(request, brukerTokenInfo)
        val resultat = beregnEtteroppgjoerResultat(request, avkorting)
        etteroppgjoerRepository.lagreAvkortingOgEtteroppgjoerResultat(request.sakId, avkorting, resultat)
        return resultat
    }

    fun hentBeregnetEtteroppgjoerResultat(
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        aar: Int,
    ): BeregnetEtteroppgjoerResultat? {
        validerBehandlingerTilhoererSammeSak(forbehandlingId, sisteIverksatteBehandlingId)
        return etteroppgjoerRepository.hentEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId)
    }

    fun hentBeregnetAvkorting(
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        sakId: SakId,
        aar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBeregnetAvkorting {
        validerBehandlingerTilhoererSammeSak(forbehandlingId, sisteIverksatteBehandlingId, sakId)
        val avkortingMedForventaInntekt =
            avkortingService
                .hentAvkortingMedReparertAarsoppgjoer(
                    sakId = sakId,
                    behandlingId = sisteIverksatteBehandlingId,
                    brukerTokenInfo = brukerTokenInfo,
                ).toDto()

        val avkortingFaktiskInntekt = hentAvkortingForBehandling(forbehandlingId, aar)

        return EtteroppgjoerBeregnetAvkorting(
            avkortingMedForventaInntekt = avkortingMedForventaInntekt,
            avkortingMedFaktiskInntekt = avkortingFaktiskInntekt,
        )
    }

    private fun beregnAvkortingForbehandling(
        request: EtteroppgjoerBeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        validerBehandlingerTilhoererSammeSak(request.forbehandlingId, request.sisteIverksatteBehandling, request.sakId)
        val sanksjoner = sanksjonService.hentSanksjon(request.sisteIverksatteBehandling)

        val tidligereAarsoppgjoer =
            avkortingService
                .hentAvkortingMedReparertAarsoppgjoer(
                    sakId = request.sakId,
                    behandlingId = request.sisteIverksatteBehandling,
                    brukerTokenInfo = brukerTokenInfo,
                ).let {
                    it.aarsoppgjoer.single { aarsoppgjoer -> aarsoppgjoer.aar == request.aar }
                }

        val avkorting =
            with(request) {
                Avkorting(
                    // Trenger bare gjeldende år i en forbehandling
                    aarsoppgjoer = listOf(tidligereAarsoppgjoer),
                ).beregnEtteroppgjoer(
                    brukerTokenInfo = brukerTokenInfo,
                    aar = aar,
                    loennsinntekt = loennsinntekt,
                    afp = afp,
                    naeringsinntekt = naeringsinntekt,
                    utland = utlandsinntekt,
                    sanksjoner = sanksjoner,
                    spesifikasjon = spesifikasjon,
                    innvilgetPeriodeIEtteroppgjoersAar = innvilgetPeriodeIEtteroppgjoersAar,
                    opphoerFom = opphoerFom,
                    brukNyeReglerAvkorting = featureToggleService.isEnabled(BeregningToggles.BEREGNING_BRUK_NYE_BEREGNINGSREGLER, false),
                )
            }

        return avkorting
    }

    private fun beregnEtteroppgjoerResultat(
        request: EtteroppgjoerBeregnFaktiskInntektRequest,
        avkortingForbehandling: Avkorting,
    ): BeregnetEtteroppgjoerResultat {
        // For å sikre at rettsgebyret forblir konsekvent i senere kjøringer, setter vi regelperioden basert på etteroppgjørsåret og ikke tidspunktet for kjøringen.
        // vi skal og bruke siste gjeldene rettsgebyr for etteroppgjoersAaret dvs det som er gjeldende 31. Desember
        val fomTom = LocalDate.of(request.aar, 12, 31)
        val regelPeriode = RegelPeriode(fomTom, fomTom)

        val (vedtakReferanse, vedtakPerioder) =
            hentVedtakslisteIEtteroppgjoersAar(request.sakId, request.aar, request.sammenlignTilOgMedBehandlingId)
        val nyForbehandlingAvkorting =
            avkortingForbehandling.aarsoppgjoer.singleOrNull { it.aar == request.aar } as? Etteroppgjoer
                ?: throw InternfeilException("Avkortingen for etteroppgjøret må ha faktisk inntekt")

        val inntektsgrunnlag = nyForbehandlingAvkorting.inntekt

        val differanseGrunnlag =
            EtteroppgjoerDifferanseGrunnlag(
                FaktumNode(vedtakPerioder, "", ""),
                FaktumNode(nyForbehandlingAvkorting, request.forbehandlingId, ""),
                FaktumNode(inntektsgrunnlag, nyForbehandlingAvkorting.id, ""),
                FaktumNode(request.harDoedsfall, request.forbehandlingId, ""),
            )

        return when (
            val beregningResultat =
                beregneEtteroppgjoerRegelMedDoedsfall.eksekver(KonstantGrunnlag(differanseGrunnlag), regelPeriode)
        ) {
            is RegelkjoeringResultat.Suksess -> {
                val data =
                    beregningResultat.periodiserteResultater
                        .single()
                        .resultat.verdi

                BeregnetEtteroppgjoerResultat(
                    id = UUID.randomUUID(),
                    forbehandlingId = request.forbehandlingId,
                    sisteIverksatteBehandlingId = request.sisteIverksatteBehandling,
                    utbetaltStoenad = data.differanse.utbetaltStoenad,
                    nyBruttoStoenad = data.differanse.nyBruttoStoenad,
                    differanse = data.differanse.differanse,
                    grense = data.grense,
                    resultatType = data.resultatType,
                    tidspunkt = data.tidspunkt,
                    regelResultat = beregningResultat.toJsonNode(),
                    kilde =
                        Grunnlagsopplysning.RegelKilde(
                            navn = beregneEtteroppgjoerRegelMedDoedsfall.regelReferanse.id,
                            ts = data.tidspunkt,
                            versjon = beregningResultat.reglerVersjon,
                        ),
                    referanseAvkorting =
                        ReferanseEtteroppgjoer(
                            avkortingForbehandling = nyForbehandlingAvkorting.id,
                            avkortingSisteIverksatte = null, // ikke relevant lengre
                            vedtakReferanse = vedtakReferanse,
                        ),
                    harIngenInntekt = data.harIngenInntekt,
                    aar = request.aar,
                )
            }

            is RegelkjoeringResultat.UgyldigPeriode -> {
                throw InternfeilException("Ugyldig regler for periode: ${beregningResultat.ugyldigeReglerForPeriode}")
            }
        }
    }

    private fun validerBehandlingerTilhoererSammeSak(
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        sakId: SakId? = null,
    ): SakId =
        inTransactionIfNeeded {
            val forbehandlingSakId = forbehandlingService.hentForbehandling(forbehandlingId).sak.id
            if (sakId != null && sakId != forbehandlingSakId) {
                throw EtteroppgjoerBehandlingerTilhoererIkkeSammeSak()
            }
            val behandling =
                behandlingService.hentBehandling(sisteIverksatteBehandlingId)
                    ?: throw InternfeilException("Klarte ikke hente behandling med behandlingId=$sisteIverksatteBehandlingId")
            if (behandling.sak.id != forbehandlingSakId) {
                throw EtteroppgjoerBehandlingerTilhoererIkkeSammeSak()
            }
            forbehandlingSakId
        }

    private fun hentVedtakslisteIEtteroppgjoersAar(
        sakId: SakId,
        etteroppgjoersAar: Int,
        tilOgMedBehandlingId: UUID?,
    ): Pair<List<Long>, List<VedtakEtteroppgjoerPeriode>> {
        val vedtaksliste =
            inTransactionIfNeeded {
                vedtakEtteroppgjoerService.hentVedtakslisteIEtteroppgjoersAar(
                    sakId,
                    etteroppgjoersAar,
                    tilOgMedBehandlingId,
                )
            }

        val vedtakReferanse = vedtaksliste.map { it.vedtakId }
        val vedtakPerioder =
            vedtaksliste
                .flatMap { it.perioder }
                .filter { it.fom.year == etteroppgjoersAar || it.tom?.year == etteroppgjoersAar }

        return vedtakReferanse to vedtakPerioder
    }

    private fun hentAvkortingForBehandling(
        behandlingId: UUID,
        aar: Int,
    ): AvkortingDto? {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId)
        val aarsoppgjoer = avkorting?.aarsoppgjoer?.single { it.aar == aar }

        return when (aarsoppgjoer) {
            is AarsoppgjoerLoepende -> {
                AvkortingDto(
                    avkortingGrunnlag = aarsoppgjoer.inntektsavkorting.map { it.grunnlag.toDto() },
                    avkortetYtelse = aarsoppgjoer.avkortetYtelse.map { it.toDto() },
                )
            }

            is Etteroppgjoer -> {
                AvkortingDto(
                    avkortingGrunnlag = listOf(aarsoppgjoer.inntekt.toDto()),
                    avkortetYtelse = aarsoppgjoer.avkortetYtelse.map { it.toDto() },
                )
            }

            else -> {
                null
            }
        }
    }
}
