package no.nav.etterlatte.vilkaarsvurdering.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult.Failure
import no.nav.etterlatte.libs.common.RetryResult.Success
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.UUID

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling
    suspend fun vilkaarsvurder(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean
    suspend fun opprett(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean
    suspend fun hentSak(sakId: Long, accessToken: String): Sak
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
                is Success -> it.content
                is Failure -> {
                    logger.error("Klarte ikke hente ut behandling med id $behandlingId. ", it.lastError())
                    throw it.exceptions.last()
                }
            }
        }
    }

    override suspend fun vilkaarsvurder(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean {
        logger.info("Sjekker hvis behandling med id $behandlingId kan vilkaarsvurdere")
        val url = "$resourceUrl/behandlinger/$behandlingId/vilkaarsvurder"

        val response = if (!commit) {
            downstreamResourceClient.get(resource = Resource(clientId = clientId, url = url), accessToken = accessToken)
        } else {
            downstreamResourceClient.post(
                resource = Resource(clientId = clientId, url = url),
                accessToken = accessToken,
                postBody = "{}"
            )
        }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Behandling med id $behandlingId kan ikke vilkaarsvurderes", it.throwable)
                false
            }
        )
    }

    override suspend fun opprett(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean {
        logger.info("Sjekker hvis behandling med id $behandlingId kan settes til status opprettet")
        val url = "$resourceUrl/behandlinger/$behandlingId/opprett"

        val response = if (!commit) {
            downstreamResourceClient.get(resource = Resource(clientId = clientId, url = url), accessToken = accessToken)
        } else {
            downstreamResourceClient.post(
                resource = Resource(clientId = clientId, url = url),
                accessToken = accessToken,
                postBody = "{}"
            )
        }

        return response.mapBoth(success = { true }, failure = {
            logger.info(
                "Behandling med id $behandlingId kan ikke endres til status ${BehandlingStatus.OPPRETTET.name}",
                it.throwable
            )
            false
        })
    }

    override suspend fun hentSak(sakId: Long, accessToken: String): Sak {
        logger.info("Henter sak med id $sakId")
        try {
            val json = downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/saker/$sakId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response

            return objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Henting av sakid ($sakId) fra vedtak feilet.", e)
            throw e
        }
    }
}