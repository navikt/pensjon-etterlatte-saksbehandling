package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekt
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontend
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.vedtak.VedtakType
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
    private val grunnlagKlient: GrunnlagKlient,
    private val vedtakKlient: VedtaksvurderingKlient,
    private val avkortingReparerAarsoppgjoeret: AvkortingReparerAarsoppgjoeret,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentOpprettEllerReberegnAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): AvkortingFrontend? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val eksisterendeAvkorting = hentAvkorting(behandlingId)

        if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
            return eksisterendeAvkorting?.let {
                if (behandling.status == BehandlingStatus.BEREGNET) {
                    val reberegnetAvkorting =
                        reberegnOgLagreAvkorting(
                            behandling,
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

        val forrigeAvkorting =
            hentAvkortingForrigeBehandling(behandling, brukerTokenInfo, behandling.virkningstidspunkt().dato)
        return if (eksisterendeAvkorting == null) {
            val nyAvkorting =
                kopierOgReberegnAvkorting(behandling, forrigeAvkorting, brukerTokenInfo)
            avkortingMedTillegg(nyAvkorting, behandling, forrigeAvkorting)
        } else if (behandling.status == BehandlingStatus.BEREGNET) {
            val reberegnetAvkorting =
                reberegnOgLagreAvkorting(
                    behandling,
                    eksisterendeAvkorting,
                    brukerTokenInfo,
                    BehandlingType.REVURDERING,
                )
            avkortingMedTillegg(reberegnetAvkorting, behandling, forrigeAvkorting)
        } else {
            avkortingMedTillegg(eksisterendeAvkorting, behandling, forrigeAvkorting)
        }
    }

    fun hentAvkorting(behandlingId: UUID) = avkortingRepository.hentAvkorting(behandlingId)

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

        // eksplisitt opphørFom overgår aldersovergang
        val aldersovergangMaaned =
            when (behandling.opphoerFraOgMed) {
                null -> {
                    val aldersovergang =
                        grunnlagKlient.aldersovergangMaaned(behandling.sak, behandling.sakType, brukerTokenInfo)
                    when (aldersovergang.year) {
                        lagreGrunnlag.fom.year -> aldersovergang
                        else -> null
                    }
                }

                else -> null
            }

        val beregnetAvkorting =
            avkorting.beregnAvkortingMedNyttGrunnlag(
                lagreGrunnlag,
                brukerTokenInfo,
                beregning,
                sanksjoner,
                behandling.opphoerFraOgMed,
                aldersovergangMaaned,
            )

        avkortingRepository.lagreAvkorting(behandlingId, behandling.sak, beregnetAvkorting)
        val lagretAvkorting = hentAvkortingNonNull(behandling.id)
        val avkortingFrontend =
            if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
                avkortingMedTillegg(lagretAvkorting, behandling)
            } else {
                val forrigeAvkorting =
                    hentAvkortingForrigeBehandling(
                        behandling,
                        brukerTokenInfo,
                        behandling.virkningstidspunkt().dato,
                    )
                avkortingMedTillegg(
                    lagretAvkorting,
                    behandling,
                    forrigeAvkorting,
                )
            }

        settBehandlingStatusAvkortet(brukerTokenInfo, behandling, behandling.behandlingType, lagretAvkorting)
        return avkortingFrontend
    }

    fun slettAvkorting(behandlingId: UUID) = avkortingRepository.slettForBehandling(behandlingId)

    /*
     * Brukes ved omregning
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
        return kopierOgReberegnAvkorting(behandling, forrigeAvkorting, brukerTokenInfo)
    }

    private suspend fun kopierOgReberegnAvkorting(
        behandling: DetaljertBehandling,
        forrigeAvkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        val opphoerFraOgMed = behandling.opphoerFraOgMed
        val kopiertAvkorting = forrigeAvkorting.kopierAvkorting(opphoerFraOgMed)
        return reberegnOgLagreAvkorting(
            behandling,
            kopiertAvkorting,
            brukerTokenInfo,
            behandling.behandlingType,
        )
    }

    private suspend fun reberegnOgLagreAvkorting(
        behandling: DetaljertBehandling,
        avkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingType: BehandlingType,
    ): Avkorting {
        tilstandssjekk(behandling.id, brukerTokenInfo)
        val beregning = beregningService.hentBeregningNonnull(behandling.id)
        val sanksjoner = sanksjonService.hentSanksjon(behandling.id) ?: emptyList()
        val beregnetAvkorting = avkorting.beregnAvkortingRevurdering(beregning, sanksjoner)
        avkortingRepository.lagreAvkorting(behandling.id, behandling.sak, beregnetAvkorting)
        val lagretAvkorting = hentAvkortingNonNull(behandling.id)
        settBehandlingStatusAvkortet(brukerTokenInfo, behandling, behandlingType, lagretAvkorting)
        return lagretAvkorting
    }

    private suspend fun settBehandlingStatusAvkortet(
        brukerTokenInfo: BrukerTokenInfo,
        behandling: DetaljertBehandling,
        behandlingType: BehandlingType,
        lagretAvkorting: Avkorting,
    ) {
        val virkningsAar = behandling.virkningstidspunkt().dato.year
        // TODO skal forenkles.. EY-4632
        if (behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING &&
            virkningsAar == 2024 &&
            (behandling.opphoerFraOgMed == null || behandling.opphoerFraOgMed!! > YearMonth.of(2025, 1)) &&
            featureToggleService.isEnabled(AvkortingToggles.VALIDERE_AARSINNTEKT_NESTE_AAR, defaultValue = false)
        ) {
            if (lagretAvkorting.aarsoppgjoer.size == 2) {
                behandlingKlient.avkort(behandling.id, brukerTokenInfo, true)
            }
        } else {
            behandlingKlient.avkort(behandling.id, brukerTokenInfo, true)
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

    suspend fun hentAvkortingForrigeBehandling(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        virkningstidspunkt: YearMonth,
    ): Avkorting {
        val alleVedtak = vedtakKlient.hentIverksatteVedtak(behandling.sak, brukerTokenInfo)
        val forrigeBehandlingId =
            alleVedtak
                .filter {
                    it.vedtakType != VedtakType.OPPHOER
                }.maxBy {
                    it.datoAttestert ?: throw InternfeilException("Iverksatt vedtak mangler dato attestert")
                }.behandlingId
        val forrigeAvkorting = hentForrigeAvkorting(forrigeBehandlingId)

        if (behandling.status == BehandlingStatus.IVERKSATT) {
            return forrigeAvkorting
        }
        return avkortingReparerAarsoppgjoeret.hentSisteAvkortingMedReparertAarsoppgjoer(
            forrigeAvkorting,
            virkningstidspunkt,
            behandling.sak,
            alleVedtak,
        )
    }

    fun hentForrigeAvkorting(forrigeBehandlingId: UUID): Avkorting =
        avkortingRepository.hentAvkorting(forrigeBehandlingId)
            ?: throw TidligereAvkortingFinnesIkkeException(forrigeBehandlingId)

    suspend fun tilstandssjekk(
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
