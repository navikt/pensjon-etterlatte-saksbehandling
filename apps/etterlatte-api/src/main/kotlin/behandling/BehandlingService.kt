package no.nav.etterlatte.behandling

import org.slf4j.LoggerFactory

class BehandlingService(private val klient: BehandlingKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentPerson(fnr: String, accessToken: String): BehandlingPersonResult {
        logger.info("Henter person fra behandling")
        return klient.hentPerson(fnr, accessToken)
    }

}