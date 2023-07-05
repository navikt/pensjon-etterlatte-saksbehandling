package no.nav.etterlatte.trygdetid.klienter

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
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.*

class BehandlingKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class BehandlingKlient(config: Config, httpClient: HttpClient) : BehandlingTilgangsSjekk {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)
    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean {
        try {
            logger.info("Sjekker tilgang til behandling $behandlingId")

            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/tilgang/behandling/$behandlingId"
                    ),
                    brukerTokenInfo = bruker
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Sjekking av tilgang for behandling feilet", e)
        }
    }

    suspend fun kanBeregnes(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): Boolean {
        logger.info("Sjekker om behandling med behandlingId=$behandlingId kan beregnes")
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/beregn")

        return downstreamResourceClient.get(resource, brukerTokenInfo)
            .mapBoth(
                success = { true },
                failure = {
                    logger.info("Behandling med id $behandlingId kan ikke beregnes")
                    false
                }
            )
    }

    suspend fun hentSisteIverksatteBehandling(sakId: Long, brukerTokenInfo: BrukerTokenInfo): UUID {
        logger.info("Henter seneste iverksatte behandling for sak med id $sakId")

        val response = downstreamResourceClient.get(
            resource = Resource(clientId = clientId, url = "$resourceUrl/saker/$sakId/behandlinger/sisteIverksatte"),
            brukerTokenInfo = brukerTokenInfo
        )

        return response.mapBoth(
            success = { UUID.fromString(it.response.toString()) },
            failure = {
                logger.error("Kunne ikke hente seneste iverksatte behandling for sak med id $sakId")
                throw it.throwable
            }
        )
    }

    suspend fun settBehandlingStatusVilkaarsvurdert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Boolean {
        logger.info("Committer vilkaarsvurdering på behandling med id $behandlingId")
        val response = downstreamResourceClient.post(
            resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/vilkaarsvurder"),
            brukerTokenInfo = brukerTokenInfo,
            postBody = "{}"
        )

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Kunne ikke committe vilkaarsvurdering på behandling med id $behandlingId", it.throwable)
                false
            }
        )
    }

    suspend fun hentBehandling(behandlingId: UUID, brukerTokenInfo: BrukerTokenInfo): DetaljertBehandling {
        logger.info("Henter behandling med behandlingId=$behandlingId")

        return retry<DetaljertBehandling> {
            downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/behandlinger/$behandlingId"
                    ),
                    brukerTokenInfo = brukerTokenInfo
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
}