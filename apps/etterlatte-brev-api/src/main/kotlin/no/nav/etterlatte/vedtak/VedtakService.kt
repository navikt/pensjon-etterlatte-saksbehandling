package no.nav.etterlatte.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory

interface VedtakService {
    suspend fun hentVedtak(behandlingId: String, accessToken: String): Vedtak
}

class VedtakServiceImpl(config: Config, httpClient: HttpClient) : VedtakService {

    private val logger = LoggerFactory.getLogger(VedtakServiceImpl::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    override suspend fun hentVedtak(behandlingId: String, accessToken: String): Vedtak {
        try {
            logger.info("Henter vedtaksvurdering (behandlingId: $behandlingId)")

            val json =
                downstreamResourceClient.get(
                    Resource(clientId, "$resourceUrl/api/behandlinger/$behandlingId/fellesvedtak"),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { errorMessage ->
                        logger.error("Henting vedtak for en behandling feilet", errorMessage.throwable)
                        null
                    }
                )?.response

            return json?.let { objectMapper.readValue(json.toString()) }!!
        } catch (e: Exception) {
            logger.error("Henting  vedtak for en behandling feilet", e)
            throw e
        }
    }
}
