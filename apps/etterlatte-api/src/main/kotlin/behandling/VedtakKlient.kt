package no.nav.etterlatte.behandling

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.*

interface EtterlatteVedtak {
    suspend fun hentVedtak(behandlingId: String, accessToken: String): Vedtak?
    suspend fun hentVedtakBolk(behandlingsidenter: List<String>, accessToken: String): List<Vedtak>
}

class VedtakKlient(config: Config, httpClient: HttpClient) : EtterlatteVedtak {
    private val logger = LoggerFactory.getLogger(VedtakKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    override suspend fun hentVedtak(behandlingId: String, accessToken: String): Vedtak? {
        logger.info("Henter vedtak for en behandling")

        try {
            val json =
                downstreamResourceClient.get(
                    Resource(clientId, "$resourceUrl/api/behandlinger/$behandlingId/vedtak"),
                    accessToken
                ).mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage ->
                        logger.error("Henting  vedtak for en behandling feilet", throwableErrorMessage)
                        null
                    }
                )?.response
            return json?.let { objectMapper.readValue(json.toString()) }
                ?: run { null }
        } catch (e: Exception) {
            logger.error("Henting  vedtak for en behandling feilet", e)
            throw e
        }
    }

    override suspend fun hentVedtakBolk(behandlingsidenter: List<String>, accessToken: String): List<Vedtak> {
        logger.info("Henter vedtak bolk")
        try {
            val json = downstreamResourceClient.post(
                Resource(clientId, "$resourceUrl/api/vedtak"),
                accessToken,
                VedtakBolkRequest(behandlingsidenter)
            )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response
            return objectMapper.readValue<VedtakBolkResponse>(json.toString()).vedtak
        } catch (e: Exception) {
            logger.error("Henting av vedtak bolk feilet", e)
            throw e
        }
    }
}

data class Vedtak(
    val sakId: String,
    val behandlingId: UUID,
    val saksbehandlerId: String?,
    val beregningsResultat: BeregningsResultat?,
    val vilkaarsResultat: JsonNode?,
    val virkningsDato: LocalDate?,
    val vedtakFattet: Boolean?,
    val datoFattet: Instant?,
    val datoattestert: Instant?,
    val attestant: String?
)

private data class VedtakBolkRequest(val behandlingsidenter: List<String>)
private data class VedtakBolkResponse(val vedtak: List<Vedtak>)