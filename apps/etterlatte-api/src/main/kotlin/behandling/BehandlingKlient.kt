package no.nav.etterlatte.behandling

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.BehandlingListe
import no.nav.etterlatte.libs.common.behandling.BehandlingSammendrag
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.typer.OppgaveListe
import no.nav.etterlatte.typer.Sak
import no.nav.etterlatte.typer.Saker
import no.nav.etterlatte.typer.LagretVedtakHendelser
import org.slf4j.LoggerFactory


interface EtterlatteBehandling {
    suspend fun hentSakerForPerson(fnr: String, accessToken: String): Saker
    suspend fun opprettSakForPerson(fnr: String, sakType: SoeknadType, accessToken: String): Sak
    suspend fun hentSaker(accessToken: String): Saker
    suspend fun hentOppgaver(accessToken: String): OppgaveListe
    suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingListe
    suspend fun hentBehandling(behandlingId: String, accessToken: String): Any
    suspend fun opprettBehandling(behandlingsBehov: BehandlingsBehov, accessToken: String): BehandlingSammendrag
    suspend fun slettBehandlinger(sakId: Int, accessToken: String): Boolean
    suspend fun hentHendelserForBehandling(behandlingId: String, accessToken: String): LagretVedtakHendelser
}

class BehandlingKlient(config: Config, httpClient: HttpClient) : EtterlatteBehandling {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")


    companion object {
        fun serialize(data: Any): String {
            return objectMapper.writeValueAsString(data)
        }
    }


    @Suppress("UNCHECKED_CAST")
    override suspend fun hentSakerForPerson(fnr: String, accessToken: String): Saker {
        try {
            logger.info("Henter saker fra behandling")
            val json = downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$resourceUrl/personer/$fnr/saker"
                    ), accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), Saker::class.java)
        } catch (e: Exception) {
            logger.error("Henting av person fra behandling feilet", e)
            throw e
        }
    }

    override suspend fun opprettSakForPerson(fnr: String, sakType: SoeknadType, accessToken: String): Sak {
        try {
            logger.info("Oppretter sak i behandling")
            val json = downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$resourceUrl/personer/$fnr/saker/$sakType"
                    ), accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), Sak::class.java)

        } catch (e: Exception) {
            logger.error("Oppretting av sak feilet", e)
            throw e
        }
    }

    override suspend fun hentSaker(accessToken: String): Saker {
        try {
            logger.info("Henter alle saker")

            val json = downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$resourceUrl/saker"
                    ), accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), Saker::class.java)
        } catch (e: Exception) {
            logger.error("Henting av saker fra behandling feilet", e)
            throw e
        }
    }

    override suspend fun hentOppgaver(accessToken: String): OppgaveListe {
        try {
            logger.info("Henter alle saker")

            val json = downstreamResourceClient
                .get(
                    Resource(
                        clientId,
                        "$resourceUrl/oppgaver"
                    ), accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString(), OppgaveListe::class.java)
        } catch (e: Exception) {
            logger.error("Henting av oppgaver fra behandling feilet", e)
            throw e
        }
    }

    override suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingListe {
        logger.info("Henter alle behandlinger i en sak")

        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/sak/$sakId/behandlinger"), accessToken)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response
            return objectMapper.readValue(json.toString(), BehandlingListe::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }

    override suspend fun hentBehandling(behandlingId: String, accessToken: String): DetaljertBehandling {
        logger.info("Henter behandling")
        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/behandlinger/$behandlingId"), accessToken)
                    .mapBoth(
                        success = { json ->
                            json
                        },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response

            logger.info("Behandling hentet for behandlingid $behandlingId: $json")
            return objectMapper.readValue(json.toString(), DetaljertBehandling::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }

    override suspend fun opprettBehandling(
        behandlingsBehov: BehandlingsBehov,
        accessToken: String
    ): BehandlingSammendrag {
        logger.info("Oppretter behandling på en sak")

        val postBody = serialize(behandlingsBehov)
        try {
            val json =
                downstreamResourceClient.post(Resource(clientId, "$resourceUrl/behandlinger"), accessToken, postBody)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response
            return objectMapper.readValue(json.toString(), BehandlingSammendrag::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }

    override suspend fun slettBehandlinger(sakId: Int, accessToken: String): Boolean {
        logger.info("Sletter behandlinger på en sak")
        try {
            downstreamResourceClient.delete(Resource(clientId, "$resourceUrl/sak/$sakId/behandlinger"), accessToken, "")
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response
            return true
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }

    override suspend fun hentHendelserForBehandling(behandlingId: String,  accessToken: String): LagretVedtakHendelser {
        logger.info("Henter hendelser for en behandling")
        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/behandlinger/$behandlingId/vedtak"), accessToken)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response

            logger.info("Vedtakhendelser hentet for behandlingid $behandlingId: $json")
            return objectMapper.readValue(json.toString(), LagretVedtakHendelser::class.java)
        } catch (e: Exception) {
            logger.error("Henting av vedtakhendelser feilet", e)
            throw e
        }
    }

}

