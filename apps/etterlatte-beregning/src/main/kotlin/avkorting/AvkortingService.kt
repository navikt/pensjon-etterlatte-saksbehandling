package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.AvkortingMapper.avkortingForFrontend
import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekter
import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.klienter.GrunnlagKlient
import no.nav.etterlatte.klienter.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontendDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonService
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

enum class InntektToggles(
    private val toggle: String,
) : FeatureToggle {
    INNTEKT_NESTE_AAR("legge-inn-flere-inntekter"),
    ;

    override fun key(): String = toggle
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

    suspend fun manglendeInntektsaar(
        behandlingId: UUID,
        avkortingSomSjekkes: Avkorting? = null,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Int> {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val avkorting = avkortingSomSjekkes ?: hentAvkorting(behandlingId)
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val aarMedAvkorting =
            avkorting
                ?.aarsoppgjoer
                .orEmpty()
                .map { it.aar }
                .toSet()

        val skalKreveInntektNesteAar =
            featureToggleService.isEnabled(
                toggleId = InntektToggles.INNTEKT_NESTE_AAR,
                defaultValue = true,
            )
        val paakrevdeAar =
            AvkortingValider
                .paakrevdeInntekterForBeregningAvAvkorting(
                    avkorting = avkorting ?: Avkorting(),
                    beregning = beregning,
                    behandlingType = behandling.behandlingType,
                    krevInntektForNesteAar = skalKreveInntektNesteAar,
                ).toSet()
        val manglendeAar = paakrevdeAar - aarMedAvkorting
        return manglendeAar.sorted()
    }

    suspend fun hentOpprettEllerReberegnAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): AvkortingFrontendDto? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val eksisterendeAvkorting = hentAvkorting(behandlingId)

        val forrigeAvkorting =
            when (behandling.behandlingType) {
                BehandlingType.FØRSTEGANGSBEHANDLING -> null
                BehandlingType.REVURDERING ->
                    hentAvkortingForrigeBehandling(
                        behandling,
                        brukerTokenInfo,
                        behandling.virkningstidspunkt().dato,
                    )
            }
        if (eksisterendeAvkorting == null && forrigeAvkorting == null) {
            return null
        }

        val manglendeInntektsaar =
            manglendeInntektsaar(
                behandlingId = behandlingId,
                avkortingSomSjekkes = eksisterendeAvkorting ?: forrigeAvkorting,
                brukerTokenInfo = brukerTokenInfo,
            )
        if (manglendeInntektsaar.isNotEmpty()) {
            logger.warn(
                "Vi har en omsstillingsstønad ${behandling.behandlingType} som mangler " +
                    "inntekt påkrevd(e) år: $manglendeInntektsaar i sak=${behandling.sak}, " +
                    "behandlingId=${behandling.id}.",
            )
            val avkorting =
                krevIkkeNull(eksisterendeAvkorting ?: forrigeAvkorting) {
                    "Både eksisterende og forrige avkorting er null, men da skulle vi returnert null fra metoden"
                }

            // Selv om vi ikke reberegner avkortingen må vi lagre ned en kopi slik at vi tar med oss det kopierte
            // når vi legger inn grunnlag senere i behandlingen.
            if (eksisterendeAvkorting == null) {
                avkortingRepository.lagreAvkorting(
                    behandlingId,
                    behandling.sak,
                    avkorting.kopierAvkorting(behandling.opphoerFraOgMed),
                )
            }
            return avkortingForFrontend(avkorting, behandling, forrigeAvkorting)
        }

        if (behandling.status == BehandlingStatus.BEREGNET && eksisterendeAvkorting != null) {
            val reberegnetAvkorting =
                reberegnOgLagreAvkorting(
                    behandling,
                    eksisterendeAvkorting,
                    brukerTokenInfo,
                )
            return avkortingForFrontend(
                reberegnetAvkorting,
                behandling,
                forrigeAvkorting,
            )
        } else if (eksisterendeAvkorting == null && forrigeAvkorting != null) {
            val nyAvkorting = kopierOgReberegnAvkorting(behandling, forrigeAvkorting, brukerTokenInfo)
            return avkortingForFrontend(
                nyAvkorting,
                behandling,
                forrigeAvkorting,
            )
        } else if (eksisterendeAvkorting != null) {
            return avkortingForFrontend(eksisterendeAvkorting, behandling, forrigeAvkorting)
        } else {
            throw InternfeilException(
                "Vi mangler både avkorting og eksisterende avkorting, som allerede er " +
                    "sjekket. Dette burde ikke skje. BehandlingId=${behandling.id}",
            )
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

    suspend fun beregnAvkortingMedNyeGrunnlag(
        behandlingId: UUID,
        nyeGrunnlag: List<AvkortingGrunnlagLagreDto>,
        brukerTokenInfo: BrukerTokenInfo,
    ): AvkortingFrontendDto {
        tilstandssjekk(behandlingId, brukerTokenInfo)
        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: Avkorting()
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        val skalKreveInntektNesteAar =
            featureToggleService.isEnabled(
                toggleId = InntektToggles.INNTEKT_NESTE_AAR,
                defaultValue = true,
            )

        validerInntekter(
            behandling,
            beregning,
            avkorting,
            nyeGrunnlag,
            skalKreveInntektNesteAar,
        )
        val aldersovergangMaaned =
            when (behandling.opphoerFraOgMed) {
                null -> {
                    val aldersovergang =
                        grunnlagKlient.aldersovergangMaaned(behandling.sak, behandling.sakType, brukerTokenInfo)
                    when (aldersovergang.year) {
                        in nyeGrunnlag.map { it.fom.year } -> aldersovergang
                        else -> null
                    }
                }

                else -> null
            }
        // liste av nye grunnlag, hvert element er for et konkret år
        // + måned bruker har aldersovergang (hvis de har det)
        val sanksjoner = sanksjonService.hentSanksjon(behandlingId)
        val oppdatert =
            avkorting.beregnAvkortingMedNyeGrunnlag(
                nyttGrunnlag = nyeGrunnlag,
                bruker = brukerTokenInfo,
                beregning = beregning,
                sanksjoner = sanksjoner ?: emptyList(),
                opphoerFom = behandling.opphoerFraOgMed,
                aldersovergang = aldersovergangMaaned,
            )

        avkortingRepository.lagreAvkorting(behandlingId, behandling.sak, oppdatert)
        val lagretAvkorting = hentAvkortingNonNull(behandling.id)
        val avkortingFrontend =
            if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
                avkortingForFrontend(lagretAvkorting, behandling)
            } else {
                val forrigeAvkorting =
                    hentAvkortingForrigeBehandling(
                        behandling,
                        brukerTokenInfo,
                        behandling.virkningstidspunkt().dato,
                    )
                avkortingForFrontend(
                    lagretAvkorting,
                    behandling,
                    forrigeAvkorting,
                )
            }

        behandlingKlient.avkort(behandling.id, brukerTokenInfo, true)
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
            behandling = behandling,
            eksisterendeAvkorting = kopiertAvkorting,
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    private fun avkortingMedOppdatertAarsoppgjoerFraForbehandling(
        forbehandlingId: UUID,
        kopiertAvkorting: Avkorting,
    ): Avkorting {
        val avkortingFraForbehandling =
            avkortingRepository.hentAvkorting(forbehandlingId)
                ?: throw InternfeilException("Mangler avkorting fra etteroppgjør forbehandling")
        val kopiertAarsoppgjoerFraForbehandling = avkortingFraForbehandling.kopierAvkorting().aarsoppgjoer.single()
        val avkortingMedErstattetAarsoppgjoer =
            kopiertAvkorting.erstattAarsoppgjoer(kopiertAarsoppgjoerFraForbehandling)

        if (kopiertAvkorting.aarsoppgjoer == avkortingMedErstattetAarsoppgjoer.aarsoppgjoer) {
            throw InternfeilException("Årsoppgjør ble ikke oppdatert med årsoppgjør fra etteroppgjør forbehandling")
        }

        return avkortingMedErstattetAarsoppgjoer
    }

    private suspend fun reberegnOgLagreAvkorting(
        behandling: DetaljertBehandling,
        eksisterendeAvkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        tilstandssjekk(behandling.id, brukerTokenInfo)
        val beregning = beregningService.hentBeregningNonnull(behandling.id)
        val sanksjoner = sanksjonService.hentSanksjon(behandling.id) ?: emptyList()

        val avkorting =
            when (behandling.revurderingsaarsak) {
                Revurderingaarsak.ETTEROPPGJOER -> {
                    val forbehandlingId =
                        behandling.relatertBehandlingId.let { UUID.fromString(it) }
                            ?: throw InternfeilException(
                                "Mangler relatertBehandlingId for revurdering etteroppgjør, " +
                                    "i behandling med id=${behandling.id} i sak=${behandling.sak}",
                            )

                    avkortingMedOppdatertAarsoppgjoerFraForbehandling(forbehandlingId, eksisterendeAvkorting)
                }

                else -> eksisterendeAvkorting
            }

        val beregnetAvkorting =
            avkorting.beregnAvkorting(
                behandling.virkningstidspunkt().dato,
                beregning,
                sanksjoner,
                behandling.opphoerFraOgMed,
            )
        avkortingRepository.lagreAvkorting(behandling.id, behandling.sak, beregnetAvkorting)
        val lagretAvkorting = hentAvkortingNonNull(behandling.id)
        behandlingKlient.avkort(behandling.id, brukerTokenInfo, true)
        return lagretAvkorting
    }

    private fun hentAvkortingNonNull(behandlingId: UUID) =
        avkortingRepository.hentAvkorting(behandlingId)
            ?: throw AvkortingFinnesIkkeException(behandlingId)

    suspend fun hentAvkortingForrigeBehandling(
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        virkningstidspunkt: YearMonth,
    ): Avkorting {
        val alleVedtak = vedtakKlient.hentIverksatteVedtak(behandling.sak, brukerTokenInfo)
        val forrigeBehandlingId =
            alleVedtak
                .filter {
                    it.vedtakType != VedtakType.OPPHOER // Opphør har ikke avkorting
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

    suspend fun hentAvkortingMedReparertAarsoppgjoer(
        sakId: SakId,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        val alleVedtak = vedtakKlient.hentIverksatteVedtak(sakId, brukerTokenInfo)
        val forrigeAvkorting = hentForrigeAvkorting(behandlingId)

        return avkortingReparerAarsoppgjoeret.hentAvkortingForSistIverksattMedReparertAarsoppgjoer(
            sakId = sakId,
            alleVedtak = alleVedtak,
            avkortingSistIverksatt = forrigeAvkorting,
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

object AvkortingMapper {
    fun avkortingForFrontend(
        avkorting: Avkorting,
        behandling: DetaljertBehandling,
        forrigeAvkorting: Avkorting? = null,
    ): AvkortingFrontendDto {
        val virkningstidspunkt = behandling.virkningstidspunkt().dato
        val redigerbareInntekter =
            if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
                // vi kan avkorte alle
                avkorting.aarsoppgjoer.map {
                    (it as? AarsoppgjoerLoepende)
                        ?.inntektsavkorting
                        ?.singleOrNull()
                        ?.grunnlag
                        ?.toDto()
                        ?: throw InternfeilException(
                            "Har en førstegangsbehandling med avkorting som har mangler " +
                                "årsoppgjør med forventet inntekt, eller har flere inntekter lagt " +
                                "inn i samme årsoppgjør. behandlingId=${behandling.id}",
                        )
                }
            } else {
                listOfNotNull(
                    avkorting.aarsoppgjoer
                        .singleOrNull { it.aar == virkningstidspunkt.year }
                        ?.let {
                            when (it) {
                                is AarsoppgjoerLoepende -> it
                                else -> null
                            }
                        }?.inntektsavkorting
                        ?.singleOrNull {
                            it.grunnlag.periode.fom == virkningstidspunkt
                        }?.grunnlag
                        ?.toDto(),
                )
            }
        val dto = avkorting.toDto(virkningstidspunkt)

        return AvkortingFrontendDto(
            redigerbareInntekter = redigerbareInntekter,
            avkortingGrunnlag = dto.avkortingGrunnlag.sortedByDescending { it.fom },
            avkortetYtelse = dto.avkortetYtelse,
            tidligereAvkortetYtelse =
                if (forrigeAvkorting != null && behandling.status != BehandlingStatus.IVERKSATT) {
                    forrigeAvkorting.aarsoppgjoer
                        .flatMap { it.avkortetYtelse }
                        .map { it.toDto() }
                } else {
                    emptyList()
                },
        )
    }
}
