package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.*


data class BehandlingSakResult(val saker: List<Sak>)
data class BehandlingSammendrag(val id: UUID, val sak: Long, val status: String)
data class Behandlinger(val behandlinger: List<BehandlingSammendrag>)


interface EtterlatteBehandling {
    suspend fun hentSakerForPerson(fnr: String, accessToken: String): BehandlingSakResult
    suspend fun opprettSakForPerson(fnr: String, sakType: String, accessToken: String): Sak
    suspend fun hentSaker(accessToken: String): BehandlingSakResult
    suspend fun hentBehandlinger(sakId: Int, accessToken: String): Behandlinger
}

class BehandlingKlient(config: Config) : EtterlatteBehandling {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)
    private val objectMapper = jacksonObjectMapper()

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")


    @Suppress("UNCHECKED_CAST")
    override suspend fun hentSakerForPerson(fnr: String, accessToken: String): BehandlingSakResult {
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

            return objectMapper.readValue(json.toString(), BehandlingSakResult::class.java)
        } catch (e: Exception) {
            logger.error("Henting av person fra behandling feilet", e)
            throw e
        }
    }

    override suspend fun opprettSakForPerson(fnr: String, sakType: String, accessToken: String): Sak {
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

    override suspend fun hentSaker(accessToken: String): BehandlingSakResult {
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

            return objectMapper.readValue(json.toString(), BehandlingSakResult::class.java)
        } catch (e: Exception) {
            logger.error("Henting av saker fra behandling feilet", e)
            throw e
        }
    }

    override suspend fun hentBehandlinger(sakId: Int, accessToken: String): Behandlinger {
        logger.info("Henter alle behandlinger i en sak")

        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/sak/$sakId/behandlinger/"), accessToken)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response

            println(json)
            return objectMapper.readValue(json.toString(), Behandlinger::class.java)
        } catch (e: Exception) {
            logger.error("Henting av behandlinger feilet", e)
            throw e
        }
    }


}

