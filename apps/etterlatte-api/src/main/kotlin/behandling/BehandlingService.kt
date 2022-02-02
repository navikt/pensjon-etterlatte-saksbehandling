package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.SoeknadType

import org.slf4j.LoggerFactory


data class PersonSakerResult(val person: Person, val saker: SakerResult)

data class BehandlingsBehov(
    val sak: Long,
    val opplysninger: List<Behandlingsopplysning>?
)

class BehandlingService(private val behandlingKlient: BehandlingKlient, private val pdlKlient: PdltjenesterKlient) {
    private val logger = LoggerFactory.getLogger(BehandlingService::class.java)

    suspend fun hentPerson(fnr: String, accessToken: String): PersonSakerResult {
        logger.info("Henter person fra behandling")

        val person = pdlKlient.hentPerson(fnr, accessToken)
        val saker = behandlingKlient.hentSakerForPerson(fnr, accessToken)

        return PersonSakerResult(person, saker)
    }

    suspend fun opprettSak(fnr: String, sakType: SoeknadType, accessToken: String): Sak {
        logger.info("Oppretter sak for en person")
        return behandlingKlient.opprettSakForPerson(fnr, sakType, accessToken)
    }

    suspend fun hentSaker(accessToken: String): SakerResult {
        logger.info("Henter alle saker")
        return behandlingKlient.hentSaker(accessToken)
    }

    suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingerSammendrag {
        logger.info("Henter behandlinger for sak $sakId")
        return behandlingKlient.hentBehandlingerForSak(sakId, accessToken)
    }

    suspend fun hentBehandling(behandlingId: String, accessToken: String): DetaljertBehandling {
        logger.info("Henter behandling")
        return behandlingKlient.hentBehandling(behandlingId, accessToken)
    }

    suspend fun opprettBehandling(behandlingsBehov: BehandlingsBehov, accessToken: String): BehandlingSammendrag {
        logger.info("Opprett en behandling på en sak")
        return behandlingKlient.opprettBehandling(behandlingsBehov, accessToken)
    }

}