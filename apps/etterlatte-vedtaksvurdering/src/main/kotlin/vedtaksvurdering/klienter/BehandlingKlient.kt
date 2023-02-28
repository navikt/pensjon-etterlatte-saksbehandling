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
import no.nav.etterlatte.token.Bruker
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, accessToken: Bruker): DetaljertBehandling
    suspend fun hentSak(sakId: Long, accessToken: Bruker): Sak

    suspend fun fattVedtak(
        behandlingId: UUID,
        accessToken: Bruker,
        vedtakHendelse: VedtakHendelse? = null
    ): Boolean
    suspend fun attester(
        behandlingId: UUID,
        accessToken: Bruker,
        vedtakHendelse: VedtakHendelse? = null
    ): Boolean
    suspend fun underkjenn(
        behandlingId: UUID,
        accessToken: Bruker,
        vedtakHendelse: VedtakHendelse? = null
    ): Boolean
}

class BehandlingKlientException(override val message: String, override val cause: Throwable? = null) :
    Exception(message, cause)

class BehandlingKlientImpl(config: Config, httpClient: HttpClient) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentBehandling(behandlingId: UUID, accessToken: Bruker): DetaljertBehandling {
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

    override suspend fun hentSak(sakId: Long, accessToken: Bruker): Sak {
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

    override suspend fun fattVedtak(
        behandlingId: UUID,
        accessToken: Bruker,
        vedtakHendelse: VedtakHendelse?
    ): Boolean {
        return if (vedtakHendelse == null) {
            statussjekkForBehandling(behandlingId, accessToken, BehandlingStatus.FATTET_VEDTAK)
        } else {
            commitStatussjekkForBehandling(behandlingId, accessToken, BehandlingStatus.FATTET_VEDTAK, vedtakHendelse)
        }
    }

    override suspend fun attester(
        behandlingId: UUID,
        accessToken: Bruker,
        vedtakHendelse: VedtakHendelse?
    ): Boolean {
        return if (vedtakHendelse == null) {
            statussjekkForBehandling(behandlingId, accessToken, BehandlingStatus.ATTESTERT)
        } else {
            commitStatussjekkForBehandling(behandlingId, accessToken, BehandlingStatus.ATTESTERT, vedtakHendelse)
        }
    }

    override suspend fun underkjenn(
        behandlingId: UUID,
        accessToken: Bruker,
        vedtakHendelse: VedtakHendelse?
    ): Boolean {
        return if (vedtakHendelse == null) {
            statussjekkForBehandling(behandlingId, accessToken, BehandlingStatus.RETURNERT)
        } else {
            commitStatussjekkForBehandling(behandlingId, accessToken, BehandlingStatus.RETURNERT, vedtakHendelse)
        }
    }

    private suspend fun statussjekkForBehandling(
        behandlingId: UUID,
        accessToken: Bruker,
        status: BehandlingStatus
    ): Boolean {
        logger.info("Sjekker behandling med behandlingId=$behandlingId til status ${status.name} (commit=false)")

        val statusnavn = getStatusNavn(status)
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/$statusnavn")

        val response = downstreamResourceClient.get(resource = resource, accessToken = accessToken)

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info(
                    "Kan ikke sjekke status=$status i behandling med behandlingId=$behandlingId (commit=false)",
                    it.throwable
                )
                false
            }
        )
    }

    private suspend fun commitStatussjekkForBehandling(
        behandlingId: UUID,
        accessToken: Bruker,
        status: BehandlingStatus,
        vedtakHendelse: VedtakHendelse
    ): Boolean {
        logger.info("Setter behandling med behandlingId=$behandlingId til status ${status.name} (commit=true)")

        val statusnavn = getStatusNavn(status)
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/$statusnavn")

        val response = downstreamResourceClient.post(
            resource = resource,
            accessToken = accessToken,
            postBody = vedtakHendelse
        )

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info(
                    "Kan ikke sette status=$status i behandling med behandlingId=$behandlingId (commit=true)",
                    it.throwable
                )
                false
            }
        )
    }

    private fun getStatusNavn(status: BehandlingStatus) =
        when (status) {
            BehandlingStatus.FATTET_VEDTAK -> "fatteVedtak"
            BehandlingStatus.ATTESTERT -> "attester"
            BehandlingStatus.RETURNERT -> "returner"
            else -> throw BehandlingKlientException("Ugyldig status ${status.name}")
        }
}