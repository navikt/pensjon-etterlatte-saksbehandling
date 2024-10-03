package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekt
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontend
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonService
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

enum class AvkortingToggles(
    val value: String,
) : FeatureToggle {
    VALIDERE_AARSINNTEKT_NESTE_AAR("validere_aarsintnekt_neste_aar"),
    ;

    override fun key(): String = this.value
}

class AvkortingService(
    private val behandlingKlient: BehandlingKlient,
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService,
    private val sanksjonService: SanksjonService,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): AvkortingFrontend? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val eksisterendeAvkorting = avkortingRepository.hentAvkorting(behandling.id)

        if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
            return eksisterendeAvkorting?.let {
                if (behandling.status == BehandlingStatus.BEREGNET) {
                    val reberegnetAvkorting =
                        reberegnOgLagreAvkorting(
                            behandling.id,
                            behandling.sak,
                            eksisterendeAvkorting,
                            brukerTokenInfo,
                            BehandlingType.FØRSTEGANGSBEHANDLING,
                        )
                    avkortingMedTillegg(reberegnetAvkorting, behandling)
                } else {
                    avkortingMedTillegg(eksisterendeAvkorting, behandling)
                }
            }
        }

        val forrigeAvkorting = hentAvkortingForrigeBehandling(behandling.sak, brukerTokenInfo)
        return if (eksisterendeAvkorting == null) {
            val nyAvkorting =
                kopierOgReberegnAvkorting(behandling, behandling.sak, forrigeAvkorting, brukerTokenInfo)
            avkortingMedTillegg(nyAvkorting, behandling, forrigeAvkorting)
        } else if (behandling.status == BehandlingStatus.BEREGNET) {
            val reberegnetAvkorting =
                reberegnOgLagreAvkorting(
                    behandling.id,
                    behandling.sak,
                    eksisterendeAvkorting,
                    brukerTokenInfo,
                    BehandlingType.REVURDERING,
                )
            avkortingMedTillegg(reberegnetAvkorting, behandling, forrigeAvkorting)
        } else {
            avkortingMedTillegg(eksisterendeAvkorting, behandling, forrigeAvkorting)
        }
    }

    suspend fun hentFullfoertAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): AvkortingDto {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val avkorting =
            avkortingRepository.hentAvkorting(behandlingId)
                ?: throw AvkortingFinnesIkkeException(behandlingId)
        return avkorting.toDto(behandling.virkningstidspunkt().dato)
    }

    suspend fun beregnAvkortingMedNyttGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        lagreGrunnlag: AvkortingGrunnlagLagreDto,
    ): AvkortingFrontend {
        tilstandssjekk(behandlingId, brukerTokenInfo)
        logger.info("Lagre og beregne avkorting og avkortet ytelse for behandlingId=$behandlingId")

        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: Avkorting()
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        validerInntekt(lagreGrunnlag, avkorting, behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING)

        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val sanksjoner = sanksjonService.hentSanksjon(behandlingId) ?: emptyList()

        val forutsettAldersovergang: YearMonth? = YearMonth.now() // TODO kalle en tjeneste som kan si om det er AO?
        val opphoerFom = forutsettAldersovergang ?: behandling.opphoerFraOgMed

        val beregnetAvkorting =
            avkorting.beregnAvkortingMedNyttGrunnlag(
                lagreGrunnlag,
                brukerTokenInfo,
                beregning,
                sanksjoner,
                opphoerFom,
            )

        avkortingRepository.lagreAvkorting(behandlingId, behandling.sak, beregnetAvkorting)
        val lagretAvkorting = hentAvkortingNonNull(behandling.id)
        val avkortingFrontend =
            if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
                avkortingMedTillegg(lagretAvkorting, behandling)
            } else {
                val forrigeAvkorting = hentAvkortingForrigeBehandling(behandling.sak, brukerTokenInfo)
                avkortingMedTillegg(
                    lagretAvkorting,
                    behandling,
                    forrigeAvkorting,
                )
            }

        settBehandlingStatusAvkortet(brukerTokenInfo, behandling.id, behandling.behandlingType, lagretAvkorting)
        return avkortingFrontend
    }

    fun slettAvkorting(behandlingId: UUID) = avkortingRepository.slettForBehandling(behandlingId)

    /*
     * Brukes ved automatisk regulering
     */
    suspend fun kopierAvkorting(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        tilstandssjekk(behandlingId, brukerTokenInfo)
        logger.info("Kopierer avkorting fra forrige behandling med behandlingId=$forrigeBehandlingId")
        val forrigeAvkorting = hentForrigeAvkorting(forrigeBehandlingId)
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        return kopierOgReberegnAvkorting(behandling, behandling.sak, forrigeAvkorting, brukerTokenInfo)
    }

    private suspend fun kopierOgReberegnAvkorting(
        behandling: DetaljertBehandling,
        sakId: SakId,
        forrigeAvkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        val opphoerFraOgMed = behandling.opphoerFraOgMed
        val kopiertAvkorting = forrigeAvkorting.kopierAvkorting(opphoerFraOgMed)
        return reberegnOgLagreAvkorting(
            behandling.id,
            sakId,
            kopiertAvkorting,
            brukerTokenInfo,
            behandling.behandlingType,
        )
    }

    private suspend fun reberegnOgLagreAvkorting(
        behandlingId: UUID,
        sakId: SakId,
        avkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingType: BehandlingType,
    ): Avkorting {
        tilstandssjekk(behandlingId, brukerTokenInfo)
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val sanksjoner = sanksjonService.hentSanksjon(behandlingId) ?: emptyList()
        val beregnetAvkorting = avkorting.beregnAvkortingRevurdering(beregning, sanksjoner)
        avkortingRepository.lagreAvkorting(behandlingId, sakId, beregnetAvkorting)
        val lagretAvkorting = hentAvkortingNonNull(behandlingId)
        settBehandlingStatusAvkortet(brukerTokenInfo, behandlingId, behandlingType, lagretAvkorting)
        return lagretAvkorting
    }

    private suspend fun settBehandlingStatusAvkortet(
        brukerTokenInfo: BrukerTokenInfo,
        behandlingId: UUID,
        behandlingType: BehandlingType,
        lagretAvkorting: Avkorting,
    ) {
        if (behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING &&
            featureToggleService.isEnabled(AvkortingToggles.VALIDERE_AARSINNTEKT_NESTE_AAR, defaultValue = false)
        ) {
            if (lagretAvkorting.aarsoppgjoer.size == 2) {
                behandlingKlient.avkort(behandlingId, brukerTokenInfo, true)
            }
        } else {
            behandlingKlient.avkort(behandlingId, brukerTokenInfo, true)
        }
    }

    private fun hentAvkortingNonNull(behandlingId: UUID) =
        avkortingRepository.hentAvkorting(behandlingId)
            ?: throw AvkortingFinnesIkkeException(behandlingId)

    private fun avkortingMedTillegg(
        avkorting: Avkorting,
        behandling: DetaljertBehandling,
        forrigeAvkorting: Avkorting? = null,
    ): AvkortingFrontend = avkorting.toFrontend(behandling.virkningstidspunkt().dato, forrigeAvkorting, behandling.status)

    private suspend fun hentAvkortingForrigeBehandling(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        val forrigeBehandlingId = behandlingKlient.hentSisteIverksatteBehandling(sakId, brukerTokenInfo).id
        return hentForrigeAvkorting(forrigeBehandlingId)
    }

    private fun hentForrigeAvkorting(forrigeBehandlingId: UUID): Avkorting =
        avkortingRepository.hentAvkorting(forrigeBehandlingId)
            ?: throw TidligereAvkortingFinnesIkkeException(forrigeBehandlingId)

    private suspend fun tilstandssjekk(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) {
        val kanAvkorte = behandlingKlient.avkort(behandlingId, bruker, commit = false)
        if (!kanAvkorte) {
            throw AvkortingBehandlingFeilStatus(behandlingId)
        }
    }
}

class AvkortingFinnesIkkeException(
    behandlingId: UUID,
) : IkkeFunnetException(
        code = "AVKORTING_IKKE_FUNNET",
        detail = "Uthenting av avkorting for behandling $behandlingId finnes ikke",
    )

class TidligereAvkortingFinnesIkkeException(
    behandlingId: UUID,
) : IkkeFunnetException(
        code = "TIDLIGERE_AVKORTING_IKKE_FUNNET",
        detail = "Fant ikke avkorting for tidligere behandling $behandlingId",
    )

class AvkortingBehandlingFeilStatus(
    behandlingId: UUID,
) : IkkeTillattException(
        code = "BEHANDLING_FEIL_STATUS_FOR_AVKORTING",
        detail = "Kan ikke avkorte da behandling med id=$behandlingId har feil status",
    )
