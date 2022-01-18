package no.nav.etterlatte.behandling

import org.slf4j.LoggerFactory

class BehandlingService(private val klient: BehandlingKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentPerson(fnr: String, accessToken: String): BehandlingPersonResult {
        logger.info("Henter person fra behandling")
        return klient.hentSakerForPerson(fnr, accessToken)
    }

    suspend fun opprettSak(fnr: String, sakType: String, accessToken: String): Boolean {
        logger.info("Oppretter sak for en person")
        return klient.opprettSakForPerson(fnr, sakType, accessToken)
    }

}