package no.nav.etterlatte.beregning.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient : BehandlingTilgangsSjekk {
    suspend fun hentBehandling(behandlingId: UUID, bruker: Bruker): DetaljertBehandling

    suspend fun hentSisteIverksatteBehandling(sakId: Long, bruker: Bruker): DetaljertBehandling
    suspend fun beregn(behandlingId: UUID, bruker: Bruker, commit: Boolean): Boolean
    suspend fun avkort(behandlingId: UUID, bruker: Bruker, commit: Boolean): Boolean
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
                        it.samlaExceptions()
                    )
                }
            }
        }
    }

    override suspend fun hentSisteIverksatteBehandling(sakId: Long, bruker: Bruker): DetaljertBehandling {
        return retry<DetaljertBehandling> {
            downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/saker/$sakId/behandlinger/sisteIverksatte"
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
                        "Klarte ikke hente siste iverksatte behandling på sak med id=$sakId",
                        it.samlaExceptions()
                    )
                }
            }
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

    override suspend fun avkort(behandlingId: UUID, bruker: Bruker, commit: Boolean): Boolean {
        logger.info("Sjekker om behandling med behandlingId=$behandlingId kan avkortes")
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/avkort")

        val response = when (commit) {
            false -> downstreamResourceClient.get(resource, bruker)
            true -> downstreamResourceClient.post(resource, bruker, "{}")
        }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Behandling med id $behandlingId kan ikke avkortes, commit=$commit")
                false
            }
        )
    }

    override suspend fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean {
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/tilgang/behandling/$behandlingId"
                    ),
                    bruker = bruker
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for behandling feilet", e)
        }
    }
}