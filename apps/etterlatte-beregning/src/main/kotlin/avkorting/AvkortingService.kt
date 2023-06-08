package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

class AvkortingService(
    private val behandlingKlient: BehandlingKlient,
    private val inntektAvkortingService: InntektAvkortingService,
    private val avkortingRepository: AvkortingRepository,
    private val beregningService: BeregningService
) {
    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)

    suspend fun hentAvkorting(behandlingId: UUID, bruker: Bruker): Avkorting? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        return avkortingRepository.hentAvkorting(behandlingId)
            ?: kopierFraForrigeBehandlingHvisRevurdering(behandlingId, bruker)
    }

    private suspend fun kopierFraForrigeBehandlingHvisRevurdering(behandlingId: UUID, bruker: Bruker): Avkorting? {
        val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)
        if (behandling.behandlingType == BehandlingType.REVURDERING) {
            val forrigeBehandling = behandlingKlient.hentSisteIverksatteBehandling(behandling.sak, bruker)
            return kopierAvkorting(behandlingId, forrigeBehandling.id, bruker)
        }
        return null
    }

    suspend fun lagreAvkorting(
        behandlingId: UUID,
        bruker: Bruker,
        avkortingGrunnlag: AvkortingGrunnlag
    ): Avkorting = tilstandssjekk(behandlingId, bruker) {
        logger.info("Lagre og beregne avkorting og avkortet ytelse for behandlingId=$behandlingId")

        val avkorting = avkortingRepository.hentAvkorting(behandlingId) ?: Avkorting.nyAvkorting(behandlingId)
        val avkortingMedNyttGrunnlag = avkorting.leggTilEllerOppdaterGrunnlag(avkortingGrunnlag)
        val beregnetAvkorting = beregnAvkorting(avkortingMedNyttGrunnlag, behandlingId, bruker)

        val lagretAvkorting = avkortingRepository.lagreAvkorting(beregnetAvkorting)
        behandlingKlient.avkort(behandlingId, bruker, true)
        lagretAvkorting
    }

    suspend fun kopierAvkorting(behandlingId: UUID, forrigeBehandlingId: UUID, bruker: Bruker): Avkorting {
        logger.info("Kopierer avkorting fra forrige behandling med behandlingId=$forrigeBehandlingId")
        val forrigeAvkorting = avkortingRepository.hentAvkorting(forrigeBehandlingId) ?: throw Exception(
            "Fant ikke avkorting for $forrigeBehandlingId"
        )
        val nyAvkorting = forrigeAvkorting.copy(
            avkortingGrunnlag = forrigeAvkorting.avkortingGrunnlag.map { it.copy(id = UUID.randomUUID()) }
        )
        val beregnetAvkorting = beregnAvkorting(nyAvkorting, behandlingId, bruker)

        val lagretAvkorting = avkortingRepository.lagreAvkorting(beregnetAvkorting)
        behandlingKlient.avkort(behandlingId, bruker, true)
        return lagretAvkorting
    }

    private suspend fun beregnAvkorting(
        avkorting: Avkorting,
        behandlingId: UUID,
        bruker: Bruker
    ): Avkorting {
        val virkningstidspunkt = behandlingKlient.hentBehandling(behandlingId, bruker).virkningstidspunkt?.dato
            ?: throw Exception("Mangler virkningstidspunkt for behandling $behandlingId")

        val avkortingsperioder = inntektAvkortingService.beregnInntektsavkorting(
            virkningstidspunkt,
            avkorting.avkortingGrunnlag
        )
        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val beregnetAvkortetYtelse = inntektAvkortingService.beregnAvkortetYtelse(
            virkningstidspunkt,
            beregning.beregningsperioder,
            avkortingsperioder
        )
        return avkorting.oppdaterAvkortingMedNyeBeregninger(avkortingsperioder, beregnetAvkortetYtelse)
    }

    private suspend fun tilstandssjekk(behandlingId: UUID, bruker: Bruker, block: suspend () -> Avkorting): Avkorting {
        val kanAvkorte = behandlingKlient.avkort(behandlingId, bruker, commit = false)
        return if (kanAvkorte) {
            block()
        } else {
            throw Exception("Kan ikke avkorte da behandlingen er i feil tilstand")
        }
    }
}