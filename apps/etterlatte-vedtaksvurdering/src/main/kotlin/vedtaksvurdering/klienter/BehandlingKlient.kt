package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.VedtakHendelse
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.*

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, accessToken: String): DetaljertBehandling
    suspend fun postVedtakHendelse(
        vedtakHendelse: VedtakHendelse,
        hendelse: String,
        behandlingId: UUID,
        accessToken: String
    ): VedtakHendelse
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
        hendelse: String,
        behandlingId: UUID,
        accessToken: String
    ): VedtakHendelse {
        logger.info("Poster hendelse $hendelse om vedtak ${vedtakHendelse.vedtakId}")
        try {
            val json = downstreamResourceClient
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

            return objectMapper.readValue(json.toString())
        } catch (exceotion: Exception) {
            logger.error(
                "Posting av vedtakhendelse ${vedtakHendelse.vedtakId} med behandlingId $behandlingId feilet.",
                exceotion
            )
            throw exceotion
        }
    }
}