package no.nav.etterlatte.beregning

import org.slf4j.LoggerFactory
import java.util.*

class AvkortingService(
    private val avkortingRepository: AvkortingRepository
) {

    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)
    fun hentAvkorting(behandlingId: UUID): Avkorting {
        logger.info("Henter avkorting for behandlingId=$behandlingId")
        return avkortingRepository.hentAvkorting(behandlingId)
    }
}