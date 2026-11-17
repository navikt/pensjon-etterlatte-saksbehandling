package no.nav.etterlatte.modules.beregning.etteroppgjoer

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandlingService
import no.nav.etterlatte.behandling.vedtaksvurdering.service.VedtakEtteroppgjoerService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransactionIfNeeded
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
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
import no.nav.etterlatte.modules.beregning.BeregningToggles
import no.nav.etterlatte.modules.beregning.avkorting.Aarsoppgjoer
import no.nav.etterlatte.modules.beregning.avkorting.AarsoppgjoerLoepende
import no.nav.etterlatte.modules.beregning.avkorting.Avkorting
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingReparerAarsoppgjoeret
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingRepository
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
    private val avkortingRepository: AvkortingRepository,
    private val sanksjonService: SanksjonService,
    private val etteroppgjoerRepository: EtteroppgjoerRepository,
    private val avkortingService: AvkortingService,
    private val reparerAarsoppgjoeret: AvkortingReparerAarsoppgjoeret,
    private val vedtakEtteroppgjoerService: VedtakEtteroppgjoerService,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
    private val forbehandlingService: EtteroppgjoerForbehandlingService,
) {
    private val logger = LoggerFactory.getLogger(EtteroppgjoerService::class.java)

    fun beregnOgLagreEtteroppgjoerResultat(
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        etteroppgjoersAar: Int,
        harDoedsfall: Boolean,
        sammenlignTilOgMedBehandlingId: UUID?,
    ): BeregnetEtteroppgjoerResultat {
        val etteroppgjoerResultat =
            beregnEtteroppgjoerResultat(
                etteroppgjoersAar,
                forbehandlingId,
                sisteIverksatteBehandlingId,
                harDoedsfall,
                sammenlignTilOgMedBehandlingId,
            )
        etteroppgjoerRepository.lagreEtteroppgjoerResultat(etteroppgjoerResultat)
        return etteroppgjoerResultat
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

    fun beregnAvkortingForbehandling(
        request: EtteroppgjoerBeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
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

        avkortingRepository.lagreAvkorting(request.forbehandlingId, request.sakId, avkorting) // TODO lagre med flagg forbehandling?
    }

    private fun beregnEtteroppgjoerResultat(
        etteroppgjoersAar: Int,
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        harDoedsfall: Boolean,
        sammenlignTilOgMedBehandlingId: UUID?,
    ): BeregnetEtteroppgjoerResultat {
        // For å sikre at rettsgebyret forblir konsekvent i senere kjøringer, setter vi regelperioden basert på etteroppgjørsåret og ikke tidspunktet for kjøringen.
        // vi skal og bruke siste gjeldene rettsgebyr for etteroppgjoersAaret dvs det som er gjeldende 31. Desember
        val fomTom = LocalDate.of(etteroppgjoersAar, 12, 31)
        val regelPeriode = RegelPeriode(fomTom, fomTom)

        val sakId = validerBehandlingerTilhoererSammeSak(forbehandlingId, sisteIverksatteBehandlingId)

        val (vedtakReferanse, vedtakPerioder) =
            hentVedtakslisteIEtteroppgjoersAar(sakId, etteroppgjoersAar, sammenlignTilOgMedBehandlingId)
        val nyForbehandlingAvkorting = finnAarsoppgjoerForEtteroppgjoer(etteroppgjoersAar, forbehandlingId)

        val inntektsgrunnlag =
            krevIkkeNull(avkortingRepository.hentFaktiskInntekt(nyForbehandlingAvkorting.id)) {
                "Avkortingen for etteroppgjøret må ha lagret et inntektsgrunnlag"
            }

        val differanseGrunnlag =
            EtteroppgjoerDifferanseGrunnlag(
                FaktumNode(vedtakPerioder, "", ""),
                FaktumNode(nyForbehandlingAvkorting, forbehandlingId, ""),
                FaktumNode(inntektsgrunnlag, nyForbehandlingAvkorting.id, ""),
                FaktumNode(harDoedsfall, forbehandlingId, ""),
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
                    forbehandlingId = forbehandlingId,
                    sisteIverksatteBehandlingId = sisteIverksatteBehandlingId,
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
                    aar = etteroppgjoersAar,
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

    private fun finnAarsoppgjoerForEtteroppgjoer(
        aar: Int,
        behandlingId: UUID,
    ): Aarsoppgjoer {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: throw GenerellIkkeFunnetException()
        val aarsoppgjoer = avkorting.aarsoppgjoer.filter { it.aar == aar }

        return when (aarsoppgjoer.size) {
            1 -> aarsoppgjoer.single()
            0 -> throw InternfeilException("Fant ikke aarsoppgjoer for $aar, selv etter reparasjon")
            else -> throw InternfeilException("Fant ${aarsoppgjoer.size} for $aar som ikke håndteres riktig")
        }
    }
}
