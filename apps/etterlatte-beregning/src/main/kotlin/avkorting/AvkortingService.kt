package no.nav.etterlatte.avkorting

import no.nav.etterlatte.avkorting.AvkortingMapper.avkortingForFrontend
import no.nav.etterlatte.avkorting.AvkortingMapper.nyAvkortingForFrontend
import no.nav.etterlatte.avkorting.AvkortingValider.validerInntekt
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
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontend
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontendDto
import no.nav.etterlatte.libs.common.beregning.AvkortingFrontendGammelDto
import no.nav.etterlatte.libs.common.beregning.AvkortingGrunnlagLagreDto
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonService
import org.slf4j.LoggerFactory
import java.time.Month
import java.time.YearMonth
import java.util.UUID

enum class AvkortingToggles(
    private val feature: String,
) : FeatureToggle {
    LEGGE_INN_FLERE_INNTEKTER("legge-inn-flere-inntekter"),
    ;

    override fun key(): String = feature
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
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Int> {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val eksisterendeAvkorting = hentAvkorting(behandlingId)
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val aarMedAvkorting = eksisterendeAvkorting?.aarsoppgjoer?.map { it.aar }?.toSet() ?: emptySet()
        val paakrevdeAar =
            AvkortingValider
                .paakrevdeInntekterForBeregningAvAvkorting(
                    avkorting = eksisterendeAvkorting ?: Avkorting(),
                    beregning = beregning,
                    behandlingType = behandling.behandlingType,
                ).toSet()
        val manglendeAar = paakrevdeAar - aarMedAvkorting
        return manglendeAar.sorted()
    }

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
                        )
                    avkortingForFrontend(
                        reberegnetAvkorting,
                        behandling,
                        skalHaInntektInnevaerendeOgNesteAar(behandling),
                    )
                } else {
                    avkortingForFrontend(
                        eksisterendeAvkorting,
                        behandling,
                        skalHaInntektInnevaerendeOgNesteAar(behandling),
                    )
                }
            }
        }
        val forrigeAvkorting =
            hentAvkortingForrigeBehandling(behandling, brukerTokenInfo, behandling.virkningstidspunkt().dato)
        return if (eksisterendeAvkorting == null) {
            val nyAvkorting =
                kopierOgReberegnAvkorting(behandling, forrigeAvkorting, brukerTokenInfo)
            avkortingForFrontend(
                nyAvkorting,
                behandling,
                skalHaInntektInnevaerendeOgNesteAar(behandling),
                forrigeAvkorting,
            )
        } else if (behandling.status == BehandlingStatus.BEREGNET) {
            val reberegnetAvkorting =
                reberegnOgLagreAvkorting(
                    behandling,
                    eksisterendeAvkorting,
                    brukerTokenInfo,
                )
            avkortingForFrontend(
                reberegnetAvkorting,
                behandling,
                skalHaInntektInnevaerendeOgNesteAar(behandling),
                forrigeAvkorting,
            )
        } else {
            avkortingForFrontend(
                eksisterendeAvkorting,
                behandling,
                skalHaInntektInnevaerendeOgNesteAar(behandling),
                forrigeAvkorting,
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
    ): AvkortingFrontend {
        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: Avkorting()
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        validerInntekter(behandling, beregning, avkorting, nyeGrunnlag)
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
                nyAvkortingForFrontend(lagretAvkorting, behandling)
            } else {
                val forrigeAvkorting =
                    hentAvkortingForrigeBehandling(
                        behandling,
                        brukerTokenInfo,
                        behandling.virkningstidspunkt().dato,
                    )
                nyAvkortingForFrontend(
                    lagretAvkorting,
                    behandling,
                    forrigeAvkorting,
                )
            }

        settBehandlingStatusAvkortet(brukerTokenInfo, behandling, lagretAvkorting)
        return avkortingFrontend
    }

    @Deprecated(
        "Bruk heller beregn avkorting med nye grunnlag når frontend støtter det",
        replaceWith = ReplaceWith("beregnAvkortingMedNyeGrunnlag(behandlingId, listOf(lagreGrunnlag), brukerTokenInfo)"),
    )
    suspend fun beregnAvkortingMedNyttGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        lagreGrunnlag: AvkortingGrunnlagLagreDto,
    ): AvkortingFrontend {
        if (featureToggleService.isEnabled(AvkortingToggles.LEGGE_INN_FLERE_INNTEKTER, false)) {
            return beregnAvkortingMedNyeGrunnlag(behandlingId, listOf(lagreGrunnlag), brukerTokenInfo)
        }

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
                avkortingForFrontend(lagretAvkorting, behandling, skalHaInntektInnevaerendeOgNesteAar(behandling))
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
                    skalHaInntektInnevaerendeOgNesteAar(behandling),
                    forrigeAvkorting,
                )
            }

        settBehandlingStatusAvkortet(brukerTokenInfo, behandling, lagretAvkorting)
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
                            ?: throw InternfeilException("Mangler relatertBehandlingId for revurdering")

                    avkortingMedOppdatertAarsoppgjoerFraForbehandling(forbehandlingId, eksisterendeAvkorting)
                }

                else -> eksisterendeAvkorting
            }

        val beregnetAvkorting = avkorting.beregnAvkorting(behandling.virkningstidspunkt().dato, beregning, sanksjoner)
        avkortingRepository.lagreAvkorting(behandling.id, behandling.sak, beregnetAvkorting)
        val lagretAvkorting = hentAvkortingNonNull(behandling.id)
        settBehandlingStatusAvkortet(brukerTokenInfo, behandling, lagretAvkorting)
        return lagretAvkorting
    }

    private suspend fun settBehandlingStatusAvkortet(
        brukerTokenInfo: BrukerTokenInfo,
        behandling: DetaljertBehandling,
        lagretAvkorting: Avkorting,
    ) {
        if (skalHaInntektInnevaerendeOgNesteAar(behandling)) {
            if (lagretAvkorting.aarsoppgjoer.size == 2) {
                behandlingKlient.avkort(behandling.id, brukerTokenInfo, true)
            }
        } else {
            behandlingKlient.avkort(behandling.id, brukerTokenInfo, true)
        }
    }

    /*
    I tilfeller der det behøves inntekt for året etter virkningsitdspunkt oppdateres status kun hvis to inntekter er lagt til.
    Dette gjelder hvis behandling er førstegangsbehandling og nåtid er fra og med oktober i innvilgelesår.
    Da anses det at nytt år er nærme nok til at søker bør kunne oppgi inntekt for nytt år.
     */
    fun skalHaInntektInnevaerendeOgNesteAar(
        behandling: DetaljertBehandling,
        naa: YearMonth = YearMonth.now(),
    ): Boolean {
        if (behandling.behandlingType != BehandlingType.FØRSTEGANGSBEHANDLING) {
            return false
        }

        val virkningstidspunkt = behandling.virkningstidspunkt().dato

        val erOpphoer =
            if (behandling.opphoerFraOgMed == null) {
                false
            } else {
                val loependeTom = behandling.opphoerFraOgMed!!.minusMonths(1)
                loependeTom.year == virkningstidspunkt.year
            }

        val virkIFjor = naa.year > virkningstidspunkt.year
        if (virkIFjor && !erOpphoer) {
            return true
        }

        val virkFraOgMedOktober = naa.year == virkningstidspunkt.year && naa.month.value >= Month.OCTOBER.value
        return virkFraOgMedOktober && !erOpphoer
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
    fun nyAvkortingForFrontend(
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
                            "Har en førstegangsbehandling med avkorting som mangler " +
                                "årsoppgjør med forventet inntekt, behandlingId=${behandling.id}",
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

    fun avkortingForFrontend(
        avkorting: Avkorting,
        behandling: DetaljertBehandling,
        skalHaInntektInnevaerendeOgNesteAar: Boolean,
        forrigeAvkorting: Avkorting? = null,
    ): AvkortingFrontendGammelDto {
        val virkningstidspunkt = behandling.virkningstidspunkt().dato
        val redigerbarForventetInntekt =
            (
                avkorting.aarsoppgjoer
                    .singleOrNull {
                        it.aar == virkningstidspunkt.year
                    }?.let {
                        when (it) {
                            is AarsoppgjoerLoepende -> it
                            else -> null
                        }
                    }
            )?.inntektsavkorting
                ?.singleOrNull {
                    it.grunnlag.periode.fom == virkningstidspunkt
                }?.grunnlag
                ?.toDto()

        val redigerbarForventetInntektNesteAar =
            if (skalHaInntektInnevaerendeOgNesteAar) {
                val nesteAar =
                    (
                        avkorting.aarsoppgjoer
                            .singleOrNull {
                                it.aar == virkningstidspunkt.year + 1
                            }?.let {
                                when (it) {
                                    is AarsoppgjoerLoepende -> it
                                    else -> null
                                }
                            }
                    )?.inntektsavkorting?.singleOrNull()?.grunnlag?.toDto()
                nesteAar
            } else {
                null
            }

        val dto = avkorting.toDto(virkningstidspunkt)

        return AvkortingFrontendGammelDto(
            redigerbarForventetInntekt = redigerbarForventetInntekt,
            redigerbarForventetInntektNesteAar = redigerbarForventetInntektNesteAar,
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
