package no.nav.etterlatte.vilkaarsvurdering.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult.Failure
import no.nav.etterlatte.libs.common.RetryResult.Success
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus.OPPRETTET
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.UUID

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling
    suspend fun opprett(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean
    suspend fun hentSak(sakId: Long, accessToken: String): Sak
    suspend fun testVilkaarsvurderingState(behandlingId: UUID, accessToken: String): Boolean
    suspend fun commitVilkaarsvurdering(
        behandlingId: UUID,
        accessToken: String,
        utfall: VilkaarsvurderingUtfall
    ): Boolean
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
                    logger.error("Klarte ikke hente ut behandling med id $behandlingId.")
                    throw it.exceptions.last()
                }
            }
        }
    }

    internal data class TilVilkaarsvurderingJson(val utfall: VilkaarsvurderingUtfall)
    override suspend fun commitVilkaarsvurdering(
        behandlingId: UUID,
        accessToken: String,
        utfall: VilkaarsvurderingUtfall
    ): Boolean {
        logger.info("Committer vilkaarsvurdering på behandling med id $behandlingId")
        val response = downstreamResourceClient.post(
            resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/vilkaarsvurder"),
            accessToken = accessToken,
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

    override suspend fun testVilkaarsvurderingState(
        behandlingId: UUID,
        accessToken: String
    ): Boolean {
        logger.info("Sjekker hvis behandling med id $behandlingId kan vilkaarsvurdere")
        val response = downstreamResourceClient.get(
            resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/vilkaarsvurder"),
            accessToken = accessToken
        )

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
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/opprett")

        val response = when (commit) {
            false -> downstreamResourceClient.get(resource = resource, accessToken = accessToken)
            true -> downstreamResourceClient.post(resource = resource, accessToken = accessToken, postBody = "{}")
        }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Behandling med id $behandlingId kan ikke endre status til ${OPPRETTET.name}", it.throwable)
                false
            }
        )
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
            logger.error("Henting av sakid ($sakId) fra vedtak feilet.")
            throw e
        }
    }
}