package no.nav.etterlatte.beregning.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling
    suspend fun beregn(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean
}

class BehandlingKlientImpl(config: Config, httpClient: HttpClient) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling {
        logger.info("Henter behandling med id $behandlingId")

        return retry<DetaljertBehandling> {
            val json = downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/behandlinger/$behandlingId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            objectMapper.readValue(json.toString())
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    logger.error("Klarte ikke hente ut behandling med id $behandlingId.")
                    throw it.exceptions.last()
                }
            }
        }
    }

    override suspend fun beregn(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean {
        logger.info("Sjekker hvis behandling med id $behandlingId kan beregnes")
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/beregn")

        val response = when (commit) {
            false -> downstreamResourceClient.get(resource, accessToken)
            true -> downstreamResourceClient.post(resource, accessToken, "{}")
        }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Behandling med id $behandlingId kan ikke beregnes, commit=$commit")
                false
            }
        )
    }
}