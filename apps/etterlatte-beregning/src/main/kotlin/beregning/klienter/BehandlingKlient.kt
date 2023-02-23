package no.nav.etterlatte.beregning.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, bruker: Bruker): DetaljertBehandling
    suspend fun hentSak(sakId: Long, bruker: Bruker): Sak
    suspend fun beregn(behandlingId: UUID, bruker: Bruker, commit: Boolean): Boolean
}

class BehandlingKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class BehandlingKlientImpl(config: Config, httpClient: HttpClient) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentBehandling(behandlingId: UUID, bruker: Bruker): DetaljertBehandling {
        logger.info("Henter behandling med behandlingId=$behandlingId")

        return retry<DetaljertBehandling> {
            downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/behandlinger/$behandlingId"
                    ),
                    bruker = bruker
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw BehandlingKlientException(
                        "Klarte ikke hente behandling med behandlingId=$behandlingId",
                        it.exceptions.last()
                    )
                }
            }
        }
    }

    override suspend fun hentSak(sakId: Long, bruker: Bruker): Sak {
        logger.info("Henter sak med id $sakId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/saker/$sakId"
                    ),
                    bruker = bruker
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av sak med sakId=$sakId fra behandling feilet", e)
        }
    }

    override suspend fun beregn(behandlingId: UUID, bruker: Bruker, commit: Boolean): Boolean {
        logger.info("Sjekker om behandling med behandlingId=$behandlingId kan beregnes")
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/beregn")

        val response = when (commit) {
            false -> downstreamResourceClient.get(resource, bruker)
            true -> downstreamResourceClient.post(resource, bruker, "{}")
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