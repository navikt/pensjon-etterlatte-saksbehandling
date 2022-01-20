package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.person.Person
import org.slf4j.LoggerFactory


data class BehandlingPersonResult (val person: Person, val saker: BehandlingSakResult)

class BehandlingService(private val behandlingKlient: BehandlingKlient, private val pdlKlient: PdltjenesterKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentPerson(fnr: String, accessToken: String): BehandlingPersonResult {
        logger.info("Henter person fra behandling")

        val person = pdlKlient.hentPerson(fnr, accessToken)
        val saker = behandlingKlient.hentSakerForPerson(fnr, accessToken)

        return BehandlingPersonResult(person, saker)
    }

    suspend fun opprettSak(fnr: String, sakType: String, accessToken: String): Sak {
        logger.info("Oppretter sak for en person")
        return behandlingKlient.opprettSakForPerson(fnr, sakType, accessToken)
    }

    suspend fun hentSaker(accessToken: String): BehandlingSakResult {
        logger.info("Henter alle saker")
        return behandlingKlient.hentSaker(accessToken)
    }

    suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): Behandlinger {
        logger.info("Henter behandlinger for sak $sakId")
        return behandlingKlient.hentBehandlinger(sakId, accessToken)
    }

}