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
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        return if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
            hentAvkorting(behandling)
        } else {
            val forrigeBehandlingId = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo).id
            val forrigeAvkorting = hentForrigeAvkorting(forrigeBehandlingId)
            return hentAvkorting(behandling, forrigeAvkorting)
                ?: kopierAvkorting(behandling, forrigeAvkorting, brukerTokenInfo)
        }
    }

    suspend fun lagreAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        avkortingGrunnlag: AvkortingGrunnlag
    ): Avkorting = tilstandssjekk(behandlingId, brukerTokenInfo) {
        logger.info("Lagre og beregne avkorting og avkortet ytelse for behandlingId=$behandlingId")

        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: Avkorting()
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val beregnetAvkorting = avkorting.beregnAvkortingMedNyttGrunnlag(
            avkortingGrunnlag,
            behandling.behandlingType,
            beregning
        )

        avkortingRepository.lagreAvkorting(behandlingId, beregnetAvkorting)
        val lagretAvkorting = if (behandling.behandlingType == BehandlingType.FØRSTEGANGSBEHANDLING) {
            hentAvkortingNonNull(behandling)
        } else {
            val forrigeBehandlingId = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, brukerTokenInfo).id
            val forrigeAvkorting = hentForrigeAvkorting(forrigeBehandlingId)
            hentAvkortingNonNull(behandling, forrigeAvkorting)
        }

        behandlingKlient.avkort(behandlingId, brukerTokenInfo, true)
        lagretAvkorting
    }

    suspend fun kopierAvkorting(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Avkorting {
        logger.info("Kopierer avkorting fra forrige behandling med behandlingId=$forrigeBehandlingId")
        val forrigeAvkorting = hentForrigeAvkorting(forrigeBehandlingId)
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)
        return kopierAvkorting(behandling, forrigeAvkorting, brukerTokenInfo)
    }

    private suspend fun kopierAvkorting(
        behandling: DetaljertBehandling,
        forrigeAvkorting: Avkorting,
        brukerTokenInfo: BrukerTokenInfo
    ): Avkorting {
        val beregning = beregningService.hentBeregningNonnull(behandling.id)
        val beregnetAvkorting = forrigeAvkorting.kopierAvkorting().beregnAvkortingRevurdering(beregning)
        avkortingRepository.lagreAvkorting(behandling.id, beregnetAvkorting)
        val lagretAvkorting = hentAvkortingNonNull(behandling, forrigeAvkorting)
        behandlingKlient.avkort(behandling.id, brukerTokenInfo, true)
        return lagretAvkorting
    }

    private fun hentAvkortingNonNull(behandling: DetaljertBehandling, forrigeAvkorting: Avkorting? = null) =
        hentAvkorting(behandling, forrigeAvkorting)
            ?: throw Exception("Uthenting av avkorting for behandling ${behandling.id} feilet")

    private fun hentAvkorting(behandling: DetaljertBehandling, forrigeAvkorting: Avkorting? = null) =
        avkortingRepository.hentAvkorting(behandling.id)?.medYtelseFraOgMedVirkningstidspunkt(
            behandling.hentVirk().dato,
            forrigeAvkorting
        )

    private fun hentForrigeAvkorting(forrigeBehandlingId: UUID): Avkorting =
        avkortingRepository.hentAvkorting(forrigeBehandlingId)
            ?: throw Exception("Fant ikke avkorting for tidligere behandling $forrigeBehandlingId")

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
    virkningstidspunkt ?: throw Exception("Mangler virkningstidspunkt for behandling $id")