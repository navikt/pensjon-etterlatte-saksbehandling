package no.nav.etterlatte.beregning

import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

class AvkortingService(
    private val avkortingRepository: AvkortingRepository
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
        avkortingRepository.lagreEllerOppdaterAvkortingGrunnlag(behandlingId, avkortingGrunnlag)
    }

    private suspend fun tilstandssjekk(behandlingId: UUID, bruker: Bruker, block: suspend () -> Avkorting): Avkorting {
        val kanAvkorte = true // TODO Legge til tilstandsjekk
        return if (kanAvkorte) {
            block()
        } else {
            throw Exception("Kan ikke avkorte da behandlingen er i feil tilstand")
        }
    }
}