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
        logger.info("Beregne avkorting og avkortet ytelse for behandlingId=$behandlingId")

        val avkortingsperioder = inntektAvkortingService.beregnInntektsavkorting(avkortingGrunnlag)

        val beregning = beregningService.hentBeregningNonnull(behandlingId)
        val beregnetAvkortetYtelse = inntektAvkortingService.beregnAvkortetYtelse(
            beregning.beregningsperioder,
            avkortingsperioder
        )

        val avkorting = avkortingRepository.lagreEllerOppdaterAvkorting(
            behandlingId,
            // TODO EY-2256 ikke sende listof her men endre repo til å appende istedenfor å slette/lagre
            listOf(avkortingGrunnlag),
            avkortingsperioder,
            beregnetAvkortetYtelse
        )
        behandlingKlient.avkort(behandlingId, bruker, true)
        avkorting
    }

    /*
    * Kopierer avkortingsgrunnlag men beregner avkorting på nytt
    */
    suspend fun kopierAvkorting(behandlingId: UUID, forrigeBehandlingId: UUID, bruker: Bruker): Avkorting {
        logger.info("Kopierer trygdetid fra forrige behandling med behandlingId=$forrigeBehandlingId")
        val forrigeAvkorting = avkortingRepository.hentAvkorting(forrigeBehandlingId) ?: throw Exception(
            "Fant ikke avkorting for $forrigeBehandlingId"
        )
        // TODO Antar her at det bare finnes et grunnlag tidligere enn så lenge da vi kun har
        //  førstegangsbehandling frem til EY-2256
        return lagreAvkorting(behandlingId, bruker, forrigeAvkorting.avkortingGrunnlag[0])
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