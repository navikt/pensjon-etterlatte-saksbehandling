package no.nav.etterlatte.avkorting

import no.nav.etterlatte.beregning.BeregningService
import no.nav.etterlatte.klienter.BehandlingKlient
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

    fun hentAvkorting(behandlingId: UUID): Avkorting? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        return avkortingRepository.hentAvkorting(behandlingId)
    }

    suspend fun lagreAvkorting(
        behandlingId: UUID,
        bruker: Bruker,
        avkortingGrunnlag: AvkortingGrunnlag // TODO Endres til liste ved inntektsendring
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
            listOf(avkortingGrunnlag),
            avkortingsperioder,
            beregnetAvkortetYtelse
        )
        behandlingKlient.avkort(behandlingId, bruker, true)
        avkorting
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