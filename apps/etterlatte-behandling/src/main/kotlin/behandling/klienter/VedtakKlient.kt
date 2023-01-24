package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface VedtakKlient {
    suspend fun hentVedtak(behandlingId: String, accessToken: String): Vedtak?
}

class VedtakKlientImpl(config: Config, httpClient: HttpClient) : VedtakKlient {
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
                        logger.warn("Henting  vedtak for en behandling feilet", throwableErrorMessage)
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
}

class VedtakKlientTest : VedtakKlient {
    override suspend fun hentVedtak(behandlingId: String, accessToken: String): Vedtak? {
        TODO("Not yet implemented")
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