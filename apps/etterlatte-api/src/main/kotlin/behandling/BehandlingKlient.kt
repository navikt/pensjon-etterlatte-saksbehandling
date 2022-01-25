package no.nav.etterlatte.behandling

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.SoeknadType
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.*


data class SakerResult(val saker: List<Sak>)
data class BehandlingSammendrag(val id: UUID, val sak: Long, val status: BehandlingStatus)
data class BehandlingerSammendrag(val behandlinger: List<BehandlingSammendrag>)

interface EtterlatteBehandling {
    suspend fun hentSakerForPerson(fnr: String, accessToken: String): SakerResult
    suspend fun opprettSakForPerson(fnr: String, sakType: SoeknadType, accessToken: String): Sak
    suspend fun hentSaker(accessToken: String): SakerResult
    suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingerSammendrag
    suspend fun hentAlleBehandlinger(accessToken: String): BehandlingerSammendrag
    suspend fun opprettBehandling(behandlingsBehov: BehandlingsBehov, accessToken: String): BehandlingSammendrag
}

class BehandlingKlient(config: Config) : EtterlatteBehandling {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")


    companion object {
        fun serialize(data: Any): String {
            return objectMapper.writeValueAsString(data)
        }
    }


    @Suppress("UNCHECKED_CAST")
    override suspend fun hentSakerForPerson(fnr: String, accessToken: String): SakerResult {
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

            return objectMapper.readValue(json.toString(), SakerResult::class.java)
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

    override suspend fun hentSaker(accessToken: String): SakerResult {
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

            return objectMapper.readValue(json.toString(), SakerResult::class.java)
        } catch (e: Exception) {
            logger.error("Henting av saker fra behandling feilet", e)
            throw e
        }
    }

    override suspend fun hentAlleBehandlinger(accessToken: String): BehandlingerSammendrag {
        logger.info("Henter alle behandinger")

        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/behandlinger"), accessToken)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response

            return objectMapper.readValue(json.toString(), BehandlingerSammendrag::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }

    }

    override suspend fun hentBehandlingerForSak(sakId: Int, accessToken: String): BehandlingerSammendrag {
        logger.info("Henter alle behandlinger i en sak")

        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/sak/$sakId/behandlinger"), accessToken)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response

            println(json)
            return objectMapper.readValue(json.toString(), BehandlingerSammendrag::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }



    override suspend fun opprettBehandling(behandlingsBehov: BehandlingsBehov, accessToken: String): BehandlingSammendrag {
        logger.info("Oppretter behandling på en sak")

        val postBody = serialize(behandlingsBehov)
        try {
            val json =
                downstreamResourceClient.post(Resource(clientId, "$resourceUrl/behandlinger", ""), accessToken, postBody)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response

            println(json)
            return objectMapper.readValue(json.toString(), BehandlingSammendrag::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }



}

