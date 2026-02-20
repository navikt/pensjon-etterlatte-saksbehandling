package no.nav.etterlatte.beregning

import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.FoersteVirkDto
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sanksjon.SanksjonService
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val behandlingKlient: BehandlingKlient,
    private val beregnBarnepensjonService: BeregnBarnepensjonService,
    private val beregnOmstillingsstoenadService: BeregnOmstillingsstoenadService,
    private val beregnOverstyrBeregningService: BeregnOverstyrBeregningService,
    private val sanksjonService: SanksjonService,
) {
    private val logger = LoggerFactory.getLogger(BeregningService::class.java)

    suspend fun hentBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = hentBeregning(behandlingId).berikMedOverstyrBeregning(brukerTokenInfo)

    fun hentBeregningNonnull(behandlingId: UUID): Beregning =
        hentBeregning(behandlingId) ?: throw Exception("Mangler beregning for behandlingId=$behandlingId")

    suspend fun opprettBeregning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Beregning {
        logger.info("Oppretter beregning for behandlingId=$behandlingId")
        val kanBeregneYtelse = behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo, commit = false)
        if (kanBeregneYtelse) {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

            val overstyrBeregning = hentOverstyrBeregning(behandling)
            if (behandling.sakType == SakType.OMSTILLINGSSTOENAD && behandling.behandlingType == BehandlingType.REVURDERING) {
                sanksjonService.kopierSanksjon(behandlingId, brukerTokenInfo)
            }
            val beregning =
                if (overstyrBeregning != null) {
                    beregnOverstyrBeregningService.beregn(behandling, overstyrBeregning, brukerTokenInfo)
                } else {
                    when (behandling.sakType) {
                        SakType.BARNEPENSJON -> {
                            beregnBarnepensjonService.beregn(
                                behandling,
                                brukerTokenInfo,
                                tilDato = behandling.opphoerFraOgMed?.minusMonths(1)?.atEndOfMonth(),
                            )
                        }

                        SakType.OMSTILLINGSSTOENAD -> {
                            beregnOmstillingsstoenadService.beregn(
                                behandling,
                                brukerTokenInfo,
                                tilDato = behandling.opphoerFraOgMed?.minusMonths(1)?.atEndOfMonth(),
                            )
                        }
                    }
                }

            val lagretBeregning = beregningRepository.lagreEllerOppdaterBeregning(beregning)
            behandlingKlient.kanBeregnes(behandlingId, brukerTokenInfo, commit = true)
            return lagretBeregning.berikMedOverstyrBeregning(brukerTokenInfo) ?: lagretBeregning
        } else {
            throw IllegalStateException("Kan ikke beregne behandlingId=$behandlingId, behandling er i feil tilstand")
        }
    }

    fun hentOverstyrBeregning(behandling: DetaljertBehandling): OverstyrBeregning? =
        beregningRepository.hentOverstyrBeregning(behandling.sak)

    suspend fun hentOverstyrBeregningPaaBehandlingId(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregning? {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        return beregningRepository.hentOverstyrBeregning(behandling.sak)
    }

    suspend fun opprettOverstyrBeregning(
        behandlingId: UUID,
        overstyrBeregning: OverstyrBeregningDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregning? {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        if (behandling.behandlingType === BehandlingType.REVURDERING) {
            validerFoersteVirke(behandlingKlient, behandling, brukerTokenInfo)
        }

        if (behandlingKlient.kanSetteStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)) {
            return hentOverstyrBeregning(behandling).takeIf { it != null } ?: run {
                val opprettetOverstyrtBeregning =
                    beregningRepository.opprettOverstyrBeregning(
                        OverstyrBeregning(
                            behandling.sak,
                            overstyrBeregning.beskrivelse,
                            Tidspunkt.now(),
                            kategori = overstyrBeregning.kategori,
                        ),
                    )
                behandlingKlient.statusTrygdetidOppdatert(behandlingId, brukerTokenInfo, commit = true)
                opprettetOverstyrtBeregning
            }
        } else {
            throw KanIkkeEndreOverstyrtBeregningGrunnetStatus(behandlingId)
        }
    }

    suspend fun deaktiverOverstyrtberegning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        if (behandlingKlient.kanSetteStatusTrygdetidOppdatert(behandlingId, brukerTokenInfo)) {
            val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

            if (behandling.behandlingType === BehandlingType.REVURDERING) {
                validerFoersteVirke(behandlingKlient, behandling, brukerTokenInfo)
            }

            beregningRepository.deaktiverOverstyrtBeregning(behandling.sak)
            beregningRepository.slettOverstyrtBeregningsgrunnlag(behandling.id)
            behandlingKlient.statusTrygdetidOppdatert(behandlingId, brukerTokenInfo, commit = true)
        } else {
            throw KanIkkeEndreOverstyrtBeregningGrunnetStatus(behandlingId)
        }
    }

    private fun hentBeregning(behandlingId: UUID): Beregning? {
        logger.info("Henter beregning for behandlingId=$behandlingId")
        return beregningRepository.hent(behandlingId)
    }

    private suspend fun Beregning?.berikMedOverstyrBeregning(brukerTokenInfo: BrukerTokenInfo) =
        this?.copy(overstyrBeregning = hentOverstyrBeregningPaaBehandlingId(behandlingId, brukerTokenInfo))

    private suspend fun validerFoersteVirke(
        behandlingKlient: BehandlingKlient,
        behandling: DetaljertBehandling,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val foersteVirkDto: FoersteVirkDto? = behandlingKlient.hentFoersteVirkningsdato(behandling.sak, brukerTokenInfo)

        if (foersteVirkDto != null && YearMonth.from(foersteVirkDto.foersteIverksatteVirkISak) != behandling.virkningstidspunkt?.dato) {
            throw KanIkkeAktivereOverstyrtBeregningGrunnetVirkningsdato()
        }
    }
}

class KanIkkeAktivereOverstyrtBeregningGrunnetVirkningsdato :
    UgyldigForespoerselException(
        code = "UGYLDIG_FOERSTE_VIRKNINGSTIDSPUNKT",
        detail = "For å legge til eller fjerne overstyre beregning må behandlingen revurderes fra sakens første virkningstidspunkt",
    )

class KanIkkeEndreOverstyrtBeregningGrunnetStatus(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "UGYLDIG_STATUS_BEHANDLING",
        detail = "Behandlingen kan ikke endre overstyrt beregning da den har feil status",
        meta = mapOf("behandlingId" to behandlingId),
    )

class TrygdetidMangler(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "TRYGDETID_MANGLER",
        detail = "Trygdetid ikke satt for behandling $behandlingId",
    )

class ForeldreloesTrygdetid(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "FORELDRELOES_TRYGDETID",
        detail = "Flere avdødes trygdetid er ikke støttet for behandling $behandlingId",
    )

class BeregningsgrunnlagMangler(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "BEREGNINGSGRUNNLAG_MANGLER",
        detail =
            "Behandling med id: $behandlingId mangler beregningsgrunnlag, " +
                "sett trygdetid metode i beregningen ovenfor.",
    )

class AnvendtGrunnbeloepIkkeFunnet :
    UgyldigForespoerselException(
        code = "ANVENDT_GRUNNBELOEP_IKKE_FUNNET",
        detail = "Anvendt grunnbeløp ikke funnet for perioden",
    )

class AnvendtTrygdetidIkkeFunnet(
    fom: LocalDate,
    tom: LocalDate?,
) : UgyldigForespoerselException(
        code = "ANVENDT_TRYGDETID_IKKE_FUNNET",
        detail = "Anvendt trygdetid ikke funnet for perioden $fom - $tom",
    )

class TrygdetidIkkeOpprettet :
    UgyldigForespoerselException(
        code = "MÅ_FASTSETTE_TRYGDETID",
        detail = "Mangler trygdetid, gå tilbake til trygdetidsiden for å opprette dette",
    )
