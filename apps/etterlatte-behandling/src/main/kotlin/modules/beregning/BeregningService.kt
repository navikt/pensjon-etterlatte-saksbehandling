package no.nav.etterlatte.modules.beregning

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.BehandlingStatusService
import no.nav.etterlatte.inTransactionIfNeeded
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.OverstyrBeregningDTO
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class BeregningService(
    private val beregningRepository: BeregningRepository,
    private val behandlingService: BehandlingService,
    private val beregnBarnepensjonService: BeregnBarnepensjonService,
    private val beregnOmstillingsstoenadService: BeregnOmstillingsstoenadService,
    private val beregnOverstyrBeregningService: BeregnOverstyrBeregningService,
    private val behandlingStatusService: BehandlingStatusService,
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
        inTransactionIfNeeded { behandlingStatusService.settBeregnet(behandlingId, brukerTokenInfo, dryRun = true) }
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw GenerellIkkeFunnetException()

        val overstyrBeregning = hentOverstyrBeregning(behandling)

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
        inTransactionIfNeeded { behandlingStatusService.settBeregnet(behandlingId, brukerTokenInfo, dryRun = false) }
        return lagretBeregning.berikMedOverstyrBeregning(brukerTokenInfo) ?: lagretBeregning
    }

    fun hentOverstyrBeregning(behandling: DetaljertBehandling): OverstyrBeregning? =
        beregningRepository.hentOverstyrBeregning(behandling.sak)

    suspend fun hentOverstyrBeregningPaaBehandlingId(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregning? {
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw GenerellIkkeFunnetException()

        return beregningRepository.hentOverstyrBeregning(behandling.sak)
    }

    suspend fun opprettOverstyrBeregning(
        behandlingId: UUID,
        overstyrBeregning: OverstyrBeregningDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): OverstyrBeregning? {
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw GenerellIkkeFunnetException()

        if (behandling.behandlingType === BehandlingType.REVURDERING) {
            validerFoersteVirke(behandling)
        }

        inTransactionIfNeeded { behandlingStatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, dryRun = true) }
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
            inTransactionIfNeeded { behandlingStatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, dryRun = false) }
            opprettetOverstyrtBeregning
        }
    }

    suspend fun deaktiverOverstyrtberegning(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        inTransactionIfNeeded { behandlingStatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, dryRun = true) }
        val behandling =
            inTransactionIfNeeded { behandlingService.hentDetaljertBehandling(behandlingId, brukerTokenInfo) }
                ?: throw GenerellIkkeFunnetException()

        if (behandling.behandlingType === BehandlingType.REVURDERING) {
            validerFoersteVirke(behandling)
        }

        beregningRepository.deaktiverOverstyrtBeregning(behandling.sak)
        beregningRepository.slettOverstyrtBeregningsgrunnlag(behandling.id)
        inTransactionIfNeeded { behandlingStatusService.settTrygdetidOppdatert(behandlingId, brukerTokenInfo, dryRun = false) }
    }

    private fun hentBeregning(behandlingId: UUID): Beregning? {
        logger.info("Henter beregning for behandlingId=$behandlingId")
        return beregningRepository.hent(behandlingId)
    }

    private suspend fun Beregning?.berikMedOverstyrBeregning(brukerTokenInfo: BrukerTokenInfo) =
        this?.copy(overstyrBeregning = hentOverstyrBeregningPaaBehandlingId(behandlingId, brukerTokenInfo))

    private fun validerFoersteVirke(behandling: DetaljertBehandling) {
        val foersteVirk =
            inTransactionIfNeeded { behandlingService.hentFoerstegangsbehandling(behandling.sak).virkningstidspunkt?.dato }
                ?: throw GenerellIkkeFunnetException()
        if (foersteVirk != behandling.virkningstidspunkt?.dato) {
            throw KanIkkeAktivereOverstyrtBeregningGrunnetVirkningsdato()
        }
    }
}

class KanIkkeAktivereOverstyrtBeregningGrunnetVirkningsdato :
    UgyldigForespoerselException(
        code = "UGYLDIG_FOERSTE_VIRKNINGSTIDSPUNKT",
        detail = "For å legge til eller fjerne overstyre beregning må behandlingen revurderes fra sakens første virkningstidspunkt",
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
