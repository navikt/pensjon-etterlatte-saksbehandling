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
import no.nav.etterlatte.vedtaksvurdering.HendelseType
import no.nav.etterlatte.vedtaksvurdering.VedtakHendelse
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling
    suspend fun hentSak(sakId: Long, accessToken: String): Sak
    suspend fun postVedtakHendelse(
        vedtakHendelse: VedtakHendelse,
        hendelse: HendelseType,
        behandlingId: UUID,
        accessToken: String
    )

    suspend fun fattVedtak(behandlingId: UUID, accessToken: String, commit: Boolean = false): Boolean
    suspend fun attester(behandlingId: UUID, accessToken: String, commit: Boolean = false): Boolean
    suspend fun underkjenn(behandlingId: UUID, accessToken: String, commit: Boolean = false): Boolean
}

class BehandlingKlientImpl(config: Config, httpClient: HttpClient) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling {
        logger.info("Henter behandling med id $behandlingId")
        try {
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

            return objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Henting av behandling ($behandlingId) fra vedtak feilet.", e)
            throw e
        }
    }

    override suspend fun postVedtakHendelse(
        vedtakHendelse: VedtakHendelse,
        hendelse: HendelseType,
        behandlingId: UUID,
        accessToken: String
    ) {
        logger.info("Poster hendelse $hendelse om vedtak ${vedtakHendelse.vedtakId}")
        try {
            downstreamResourceClient
                .post(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/behandlinger/$behandlingId/hendelser/vedtak/$hendelse"
                    ),
                    accessToken = accessToken,
                    postBody = vedtakHendelse
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response
        } catch (exception: Exception) {
            logger.error(
                "Posting av vedtakhendelse ${vedtakHendelse.vedtakId} med behandlingId $behandlingId feilet.",
                exception
            )
            throw exception
        }
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
        logger.info("Sjekker hvis behandling med id $behandlingId kan settes til status ${status.name}")
        val statusnavn = when (status) {
            BehandlingStatus.FATTET_VEDTAK -> "fatteVedtak"
            BehandlingStatus.ATTESTERT -> "attester"
            BehandlingStatus.RETURNERT -> "returner"
            else -> throw RuntimeException("Feilet i kall mot behandling. Forventet ikke status ${status.name}")
        }
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/$statusnavn")

        val response = when (commit) {
            true -> downstreamResourceClient.post(resource = resource, accessToken = accessToken, postBody = "{}")
            false -> downstreamResourceClient.get(resource = resource, accessToken = accessToken)
        }

        return response.mapBoth(
            success = { true },
            failure = {
                logger.error("Det skjedde en feil ved statussjekk mot behandling med id $behandlingId", it.throwable)
                false
            }
        )
    }
}