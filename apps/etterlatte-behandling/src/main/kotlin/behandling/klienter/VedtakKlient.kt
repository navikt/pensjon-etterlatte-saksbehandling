package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory

interface VedtakKlient {
    suspend fun hentVedtak(behandlingId: String, bruker: Bruker): VedtakDto?
}

class VedtakKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class VedtakKlientImpl(config: Config, httpClient: HttpClient) : VedtakKlient {
    private val logger = LoggerFactory.getLogger(VedtakKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    override suspend fun hentVedtak(behandlingId: String, bruker: Bruker): VedtakDto? {
        logger.info("Henter vedtak for behandling med behandlingId=$behandlingId")

        try {
            return downstreamResourceClient.get(
                resource = Resource(clientId, "$resourceUrl/api/vedtak/$behandlingId"),
                bruker = bruker
            ).mapBoth(
                success = { resource -> resource.response?.let { objectMapper.readValue(it.toString()) } },
                failure = { errorResponse ->
                    if (errorResponse.downstreamStatusCode == HttpStatusCode.NotFound) {
                        null
                    } else {
                        throw errorResponse
                    }
                }
            )
        } catch (e: Exception) {
            throw VedtakKlientException("Henting av vedtak for behandling med behandlingId=$behandlingId feilet", e)
        }
    }
}