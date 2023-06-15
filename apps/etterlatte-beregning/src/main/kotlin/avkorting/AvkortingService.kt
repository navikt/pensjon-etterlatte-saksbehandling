package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.*

class AvkortingService(
    private val behandlingKlient: BehandlingKlient,
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService
) {
    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)

    suspend fun hentAvkorting(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): Avkorting? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        return avkortingRepository.hentAvkorting(behandlingId)
            ?: kopierFraForrigeBehandlingHvisRevurdering(behandlingId, brukerTokenInfo)
    }

    private suspend fun kopierFraForrigeBehandlingHvisRevurdering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Avkorting? {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        if (behandling.behandlingType == BehandlingType.REVURDERING) {
            val forrigeBehandling = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo)
            return kopierAvkorting(behandlingId, forrigeBehandling.id, brukerTokenInfo)
        }
        return null
    }

    suspend fun lagreAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        avkortingGrunnlag: AvkortingGrunnlag
    ): Avkorting = tilstandssjekk(behandlingId, brukerTokenInfo) {
        logger.info("Lagre og beregne avkorting og avkortet ytelse for behandlingId=$behandlingId")

        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: Avkorting.nyAvkorting()

        val virkningstidspunkt = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo).hentVirk()
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val beregnetAvkorting = avkorting.beregnAvkortingNyttEllerEndretGrunnlag(
            avkortingGrunnlag,
            virkningstidspunkt,
            beregning
        )

        val lagretAvkorting = avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
        behandlingKlient.avkort(behandlingId, brukerTokenInfo, true)
        lagretAvkorting
    }

    suspend fun kopierAvkorting(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Avkorting {
        logger.info("Kopierer avkorting fra forrige behandling med behandlingId=$forrigeBehandlingId")
        val forrigeAvkorting = avkortingRepository.hentAvkorting(forrigeBehandlingId) ?: throw Exception(
            "Fant ikke avkorting for $forrigeBehandlingId"
        )
        val nyAvkorting = forrigeAvkorting.kopierAvkorting()

        val virkningstidspunkt = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo).hentVirk()
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val beregnetAvkorting = nyAvkorting.beregnAvkorting(virkningstidspunkt, beregning, forrigeAvkorting)

        val lagretAvkorting = avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
        behandlingKlient.avkort(behandlingId, brukerTokenInfo, true)
        return lagretAvkorting
    }

    private suspend fun tilstandssjekk(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
        block: suspend () -> Avkorting
    ): Avkorting {
        val kanAvkorte = behandlingKlient.avkort(behandlingId, bruker, commit = false)
        return if (kanAvkorte) {
            block()
        } else {
            throw Exception("Kan ikke avkorte da behandlingen er i feil tilstand")
        }
    }
}

private fun DetaljertBehandling.hentVirk() =
    virkningstidspunkt?.dato ?: throw Exception("Mangler virkningstidspunkt for behandling $id")