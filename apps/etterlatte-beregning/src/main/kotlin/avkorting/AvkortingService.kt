package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.virkningstidspunkt
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class AvkortingService(
    private val behandlingKlient: BehandlingKlient,
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService,
) {
    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)

    suspend fun hentAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val eksisterendeAvkorting = avkortingRepository.hentAvkorting(behandling.id)

        if (behandling.behandlingType == BehandlingType.FOERSTEGANGSBEHANDLING) {
            return eksisterendeAvkorting?.let {
                if (behandling.status == BehandlingStatus.BEREGNET) {
                    val reberegnetAvkorting =
                        reberegnOgLagreAvkorting(behandling.id, eksisterendeAvkorting, brukerTokenInfo)
                    avkortingMedTillegg(reberegnetAvkorting, behandling)
                } else {
                    avkortingMedTillegg(eksisterendeAvkorting, behandling)
                }
            }
        }

        val forrigeAvkorting = hentAvkortingForrigeBehandling(behandling.sak, brukerTokenInfo)
        return if (eksisterendeAvkorting == null) {
            val nyAvkorting = kopierOgReberegnAvkorting(behandling.id, forrigeAvkorting, brukerTokenInfo)
            avkortingMedTillegg(nyAvkorting, behandling, forrigeAvkorting)
        } else if (behandling.status == BehandlingStatus.BEREGNET) {
            val reberegnetAvkorting = reberegnOgLagreAvkorting(behandling.id, eksisterendeAvkorting, brukerTokenInfo)
            avkortingMedTillegg(reberegnetAvkorting, behandling, forrigeAvkorting)
        } else {
            avkortingMedTillegg(eksisterendeAvkorting, behandling, forrigeAvkorting)
        }
    }

    suspend fun beregnAvkortingMedNyttGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        avkortingGrunnlag: AvkortingGrunnlag,
    ): Avkorting {
        tilstandssjekk(behandlingId, brukerTokenInfo)
        logger.info("Lagre og beregne avkorting og avkortet ytelse for behandlingId=$behandlingId")

        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: Avkorting()
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val beregnetAvkorting =
            avkorting.beregnAvkortingMedNyttGrunnlag(
                avkortingGrunnlag,
                behandling.behandlingType,
                beregning,
            )

        avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
        val lagretAvkorting =
            if (behandling.behandlingType == BehandlingType.FOERSTEGANGSBEHANDLING) {
                avkortingMedTillegg(hentAvkortingNonNull(behandling.id), behandling)
            } else {
                val forrigeAvkorting = hentAvkortingForrigeBehandling(behandling.sak, brukerTokenInfo)
                avkortingMedTillegg(
                    hentAvkortingNonNull(behandling.id),
                    behandling,
                    forrigeAvkorting,
                )
            }

        behandlingKlient.avkort(behandlingId, brukerTokenInfo, true)
        return lagretAvkorting
    }

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
        return kopierOgReberegnAvkorting(behandling.id, forrigeAvkorting, brukerTokenInfo)
    }

    private suspend fun kopierOgReberegnAvkorting(
        behandlingId: UUID,
        forrigeAvkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        val kopiertAvkorting = forrigeAvkorting.kopierAvkorting()
        return reberegnOgLagreAvkorting(behandlingId, kopiertAvkorting, brukerTokenInfo)
    }

    private suspend fun reberegnOgLagreAvkorting(
        behandlingId: UUID,
        avkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo,
    ): Avkorting {
        tilstandssjekk(behandlingId, brukerTokenInfo)
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val beregnetAvkorting = avkorting.beregnAvkortingRevurdering(beregning)
        avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
        val lagretAvkorting = hentAvkortingNonNull(behandlingId)
        behandlingKlient.avkort(behandlingId, brukerTokenInfo, true)
        return lagretAvkorting
    }

    private fun hentAvkortingNonNull(behandlingId: UUID) =
        avkortingRepository.hentAvkorting(behandlingId)
            ?: throw AvkortingFinnesIkkeException(behandlingId)

    private fun avkortingMedTillegg(
        avkorting: Avkorting,
        behandling: DetaljertBehandling,
        forrigeAvkorting: Avkorting? = null,
    ): Avkorting {
        // Forrige behandling er forrige iverksatte som da vil være seg selv eller nyere hvis status er iverksatte
        val forrigeBehandling =
            when (behandling.status) {
                BehandlingStatus.IVERKSATT -> null
                else -> forrigeAvkorting
            }
        return avkorting.medYtelseFraOgMedVirkningstidspunkt(behandling.virkningstidspunkt().dato, forrigeBehandling)
    }

    private suspend fun hentAvkortingForrigeBehandling(
        sakId: Long,
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

class AvkortingFinnesIkkeException(behandlingId: UUID) : IkkeFunnetException(
    code = "AVKORTING_IKKE_FUNNET",
    detail = "Uthenting av avkorting for behandling $behandlingId finnes ikke",
)

class TidligereAvkortingFinnesIkkeException(behandlingId: UUID) : IkkeFunnetException(
    code = "TIDLIGERE_AVKORTING_IKKE_FUNNET",
    detail = "Fant ikke avkorting for tidligere behandling $behandlingId",
)

class AvkortingBehandlingFeilStatus(behandlingId: UUID) : IkkeTillattException(
    code = "BEHANDLING_FEIL_STATUS_FOR_AVKORTING",
    detail = "Kan ikke avkorte da behandling med id=$behandlingId har feil status",
)
