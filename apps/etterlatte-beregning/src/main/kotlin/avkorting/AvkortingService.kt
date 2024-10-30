package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekt
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontend
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.beregning.AvkortingHarInntektForAarDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonService
import org.slf4j.LoggerFactory
import java.time.LocalTime
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
                            behandling,
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
                    behandling,
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

    // Sjekke om Avkorting innvilga måneder overstyres på grunn av tidlig alderspensjon, og opprett oppgave om opphør
    suspend fun opprettOppgaveHvisTidligAlderspensjon(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        logger.info("Sjekker om vi skal opprette oppgave om opphør grunnen tidlig alderspensjon for sakId=${behandling.sak.sakId}")
        val avkorting =
            avkortingRepository.hentAvkorting(behandlingId) ?: throw AvkortingFinnesIkkeException(behandlingId)

        val aarsoppgjoer = avkorting.aarsoppgjoer.single { it.aar == behandling.virkningstidspunkt?.dato?.year }
        val overstyrtInntektsavkorting =
            aarsoppgjoer.inntektsavkorting
                .find {
                    it.grunnlag.overstyrtInnvilgaMaanederAarsak == OverstyrtInnvilgaMaanederAarsak.TAR_UT_PENSJON_TIDLIG &&
                        it.grunnlag.periode.fom == behandling.virkningstidspunkt?.dato
                }

        if (overstyrtInntektsavkorting != null) {
            logger.info("Oppretter oppgave om opphør grunnen tidlig alderspensjon for sakId=${behandling.sak.sakId}")

            // Vi trekker fra 2 måneder (2L) for å få måneden før opphør (2024.03).
            // For eksempel, hvis fom = 2024.1 og innvilgaMaaneder = 3:
            // 01 + 03 = 4 (2024.04), så 4 - 2L = 2 gir oss 2024.02 som er en månede før 2024.03.
            val innvilgaMaaneder = overstyrtInntektsavkorting.grunnlag.innvilgaMaaneder - 2L
            val oppgaveFrist =
                aarsoppgjoer.fom
                    .plusMonths(innvilgaMaaneder)
                    .atDay(1)

            behandlingKlient.opprettOppgave(
                behandling.sak,
                brukerTokenInfo,
                OppgaveType.GENERELL_OPPGAVE,
                "Opphør av ytelse på grunn av alderspensjon.",
                Tidspunkt.ofNorskTidssone(oppgaveFrist, LocalTime.now()),
                OppgaveKilde.BEHANDLING,
                behandlingId.toString(),
            )
        } else {
            logger.info("Fant ingen tidlig alderspensjon for sakId=${behandling.sak.sakId}, trenger ingen oppgave om opphør.")
        }
    }

    fun hentHarSakInntektForAar(harInntektForAarDto: AvkortingHarInntektForAarDto): Boolean =
        avkortingRepository.harSakInntektForAar(harInntektForAarDto)

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
                val forrigeAvkorting = hentAvkortingForrigeBehandling(behandling.sak, brukerTokenInfo)
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
            behandling,
            sakId,
            kopiertAvkorting,
            brukerTokenInfo,
            behandling.behandlingType,
        )
    }

    private suspend fun reberegnOgLagreAvkorting(
        behandling: DetaljertBehandling,
        sakId: SakId,
        avkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo,
        behandlingType: BehandlingType,
    ): Avkorting {
        tilstandssjekk(behandling.id, brukerTokenInfo)
        val beregning = beregningService.hentBeregningNonnull(behandling.id)
        val sanksjoner = sanksjonService.hentSanksjon(behandling.id) ?: emptyList()
        val beregnetAvkorting = avkorting.beregnAvkortingRevurdering(beregning, sanksjoner)
        avkortingRepository.lagreAvkorting(behandling.id, sakId, beregnetAvkorting)
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
        if (behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING &&
            featureToggleService.isEnabled(AvkortingToggles.VALIDERE_AARSINNTEKT_NESTE_AAR, defaultValue = false)
        ) {
            val opphoerSammeAar =
                behandling.opphoerFraOgMed?.let { it.year == behandling.virkningstidspunkt().dato.year } ?: false
            if (opphoerSammeAar || lagretAvkorting.aarsoppgjoer.size == 2) {
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
