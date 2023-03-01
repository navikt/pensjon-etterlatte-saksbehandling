package no.nav.etterlatte.vilkaarsvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.OPPRETTET
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, bruker: Bruker): DetaljertBehandling
    suspend fun kanSetteBehandlingStatusVilkaarsvurdert(behandlingId: UUID, bruker: Bruker): Boolean
    suspend fun settBehandlingStatusOpprettet(behandlingId: UUID, bruker: Bruker, commit: Boolean): Boolean
    suspend fun settBehandlingStatusVilkaarsvurdert(
        behandlingId: UUID,
        bruker: Bruker,
        utfall: VilkaarsvurderingUtfall
    ): Boolean
}

class BehandlingKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

internal data class TilVilkaarsvurderingJson(val utfall: VilkaarsvurderingUtfall)

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

    override suspend fun kanSetteBehandlingStatusVilkaarsvurdert(
        behandlingId: UUID,
        bruker: Bruker
    ): Boolean {
        logger.info("Sjekker om behandling med id $behandlingId kan vilkaarsvurdere")
        val response = downstreamResourceClient.get(
            resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/vilkaarsvurder"),
            bruker = bruker
        )

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Behandling med id $behandlingId kan ikke vilkaarsvurderes", it.throwable)
                false
            }
        )
    }

    override suspend fun settBehandlingStatusVilkaarsvurdert(
        behandlingId: UUID,
        bruker: Bruker,
        utfall: VilkaarsvurderingUtfall
    ): Boolean {
        logger.info("Committer vilkaarsvurdering på behandling med id $behandlingId")
        val response = downstreamResourceClient.post(
            resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/vilkaarsvurder"),
            bruker = bruker,
            postBody = TilVilkaarsvurderingJson(utfall)
        )

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Kunne ikke committe vilkaarsvurdering på behandling med id $behandlingId", it.throwable)
                false
            }
        )
    }

    override suspend fun settBehandlingStatusOpprettet(
        behandlingId: UUID,
        bruker: Bruker,
        commit: Boolean
    ): Boolean {
        logger.info("Setter behandling med id $behandlingId til status ${OPPRETTET.name} (commit=$commit)")
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/opprett")

        val response = when (commit) {
            false -> downstreamResourceClient.get(resource = resource, bruker = bruker)
            true -> downstreamResourceClient.post(resource = resource, bruker = bruker, postBody = "{}")
        }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info(
                    "Behandling med id $behandlingId kan ikke endre status til ${OPPRETTET.name} (commit=$commit)",
                    it.throwable
                )
                false
            }
        )
    }
}