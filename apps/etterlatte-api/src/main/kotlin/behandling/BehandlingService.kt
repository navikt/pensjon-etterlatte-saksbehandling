package no.nav.etterlatte.behandling

import org.slf4j.LoggerFactory

class BehandlingService(private val klient: BehandlingKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentPerson(fnr: String): String {
        logger.info("Henter person fra behandling")
        val response = klient.hentPerson(fnr)

        return "ikke implementert"
    }

}