package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling
    suspend fun hentSak(sakId: Long, accessToken: String): Sak

    suspend fun fattVedtak(behandlingId: UUID, accessToken: String, commit: Boolean = false): Boolean
    suspend fun attester(behandlingId: UUID, accessToken: String, commit: Boolean = false): Boolean
    suspend fun underkjenn(behandlingId: UUID, accessToken: String, commit: Boolean = false): Boolean
}

class BehandlingKlientException(override val message: String, override val cause: Throwable? = null) :
    Exception(message, cause)

class BehandlingKlientImpl(config: Config, httpClient: HttpClient) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling {
        logger.info("Henter behandling med behandlingId=$behandlingId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/behandlinger/$behandlingId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av behandling med behandlingId=$behandlingId feilet", e)
        }
    }

    override suspend fun hentSak(sakId: Long, accessToken: String): Sak {
        logger.info("Henter sak med sakId=$sakId")
        try {
            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/saker/$sakId"
                    ),
                    accessToken = accessToken
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw BehandlingKlientException("Henting av sak med sakId=$sakId feilet")
        }
    }

    override suspend fun fattVedtak(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean {
        return statussjekkForBehandling(behandlingId, accessToken, commit, BehandlingStatus.FATTET_VEDTAK)
    }

    override suspend fun attester(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean {
        return statussjekkForBehandling(behandlingId, accessToken, commit, BehandlingStatus.ATTESTERT)
    }

    override suspend fun underkjenn(behandlingId: UUID, accessToken: String, commit: Boolean): Boolean {
        return statussjekkForBehandling(behandlingId, accessToken, commit, BehandlingStatus.RETURNERT)
    }

    private suspend fun statussjekkForBehandling(
        behandlingId: UUID,
        accessToken: String,
        commit: Boolean,
        status: BehandlingStatus
    ): Boolean {
        logger.info("Setter behandling med behandlingId=$behandlingId til status ${status.name} (commit=$commit)")

        val statusnavn = when (status) {
            BehandlingStatus.FATTET_VEDTAK -> "fatteVedtak"
            BehandlingStatus.ATTESTERT -> "attester"
            BehandlingStatus.RETURNERT -> "returner"
            else -> throw BehandlingKlientException("Ugyldig status ${status.name}")
        }
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/$statusnavn")

        val response = when (commit) {
            true -> downstreamResourceClient.post(resource = resource, accessToken = accessToken, postBody = "{}")
            false -> downstreamResourceClient.get(resource = resource, accessToken = accessToken)
        }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info(
                    "Kan ikke sette status=$status i behandling med behandlingId=$behandlingId (commit=$commit)",
                    it.throwable
                )
                false
            }
        )
    }
}