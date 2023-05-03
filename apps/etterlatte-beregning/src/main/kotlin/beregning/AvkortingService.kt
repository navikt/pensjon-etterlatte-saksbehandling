package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

class AvkortingService(
    private val avkortingRepository: AvkortingRepository,
    private val behandlingKlient: BehandlingKlient,
    private val inntektAvkortingService: InntektAvkortingService
) {
    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)

    fun hentAvkorting(behandlingId: UUID): Avkorting? {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        return avkortingRepository.hentAvkorting(behandlingId)
    }

    suspend fun lagreAvkortingGrunnlag(
        behandlingId: UUID,
        bruker: Bruker,
        avkortingGrunnlag: AvkortingGrunnlag
    ): Avkorting = tilstandssjekk(behandlingId, bruker) {
        logger.info("Lagre grunnlag for avkorting for behandlingId=$behandlingId")

        val inntektavkorting = inntektAvkortingService.beregnInntektsavkorting(avkortingGrunnlag)

        avkortingRepository.lagreEllerOppdaterAvkortingGrunnlag(
            behandlingId,
            avkortingGrunnlag.copy(beregnetAvkorting = inntektavkorting)
        )
    }

    private suspend fun tilstandssjekk(behandlingId: UUID, bruker: Bruker, block: suspend () -> Avkorting): Avkorting {
        val kanAvkorte = behandlingKlient.beregn(behandlingId, bruker, commit = false)
        return if (kanAvkorte) {
            block()
        } else {
            throw Exception("Kan ikke avkorte da behandlingen er i feil tilstand")
        }
    }
}