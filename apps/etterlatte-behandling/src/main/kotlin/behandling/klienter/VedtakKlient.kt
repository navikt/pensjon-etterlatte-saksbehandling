package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface VedtakKlient {
    suspend fun hentVedtak(behandlingId: String, bruker: Bruker): Vedtak?
}

class VedtakKlientException(override val message: String, override val cause: Throwable) : Exception(message, cause)

class VedtakKlientImpl(config: Config, httpClient: HttpClient) : VedtakKlient {
    private val logger = LoggerFactory.getLogger(VedtakKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    override suspend fun hentVedtak(behandlingId: String, bruker: Bruker): Vedtak? {
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

data class BeregningsResultat(
    val id: UUID,
    val type: Beregningstype,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: LocalDateTime,
    val grunnlagVersjon: Long = 0L
)