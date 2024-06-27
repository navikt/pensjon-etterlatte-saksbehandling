package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.OppdaterSamordningsmelding
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.Samordningsvedtak
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface SamordningsKlient {
    /**
     * @return true=vente på samordning med tjenestepensjon, false=ikke vente
     */
    suspend fun samordneVedtak(
        vedtak: Vedtak,
        etterbetaling: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    /**
     * @param vedtak IDer som sendes til SAM
     */
    suspend fun hentSamordningsdata(
        vedtak: Vedtak,
        alleVedtak: Boolean,
    ): List<Samordningsvedtak>

    suspend fun oppdaterSamordningsmelding(
        samordningmelding: OppdaterSamordningsmelding,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

class SamordningsKlientImpl(
    config: Config,
    private val httpClient: HttpClient,
) : SamordningsKlient {
    private val logger = LoggerFactory.getLogger(SamordningsKlient::class.java)

    private val resourceUrl = config.getString("samordnevedtak.resource.url")

    override suspend fun samordneVedtak(
        vedtak: Vedtak,
        etterbetaling: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        try {
            val response =
                httpClient.post("$resourceUrl/api/vedtak/samordne") {
                    contentType(ContentType.Application.Json)
                    setBody(vedtak.tilSamordneRequest(etterbetaling))
                }

            if (response.status.isSuccess()) {
                return response.body<String>().let { objectMapper.readValue<SamordneVedtakRespons>(it) }.ventPaaSvar
            } else {
                throw ResponseException(response, "Samordne vedtak feilet [id=${vedtak.id}]")
            }
        } catch (e: Exception) {
            logger.error("Samordne vedtak feilet [id=${vedtak.id}]", e)
            throw SamordneVedtakGenerellException("Samordne vedtak feilet [id=${vedtak.id}]", e)
        }
    }

    override suspend fun hentSamordningsdata(
        vedtak: Vedtak,
        alleVedtak: Boolean,
    ): List<Samordningsvedtak> {
        try {
            val response =
                httpClient.get("$resourceUrl/api/vedtak") {
                    header("pid", vedtak.soeker.value)
                    parameter("fagomrade", "EYO")
                    // parameter("sakId", "${vedtak.sakId}")  // FIXME retting i SAM ble prodsatt 29.05.2024. 6 ukers svarfrist...
                    if (!alleVedtak) {
                        parameter("vedtakId", "${vedtak.id}")
                    }
                }

            return when (response.status) {
                HttpStatusCode.OK -> response.body<List<Samordningsvedtak>>()
                HttpStatusCode.NoContent -> emptyList()
                else -> throw ResponseException(response, "Kunne ikke hente samordningsdata")
            }
        } catch (e: Exception) {
            throw SamordneVedtakGenerellException("Kunne ikke hente samordningsdata", e)
        }
    }

    override suspend fun oppdaterSamordningsmelding(
        samordningmelding: OppdaterSamordningsmelding,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        try {
            val response =
                httpClient.post("$resourceUrl/api/refusjonskrav") {
                    contentType(ContentType.Application.Json)
                    header("pid", samordningmelding.pid)
                    parameter("tpNr", samordningmelding.tpNr)
                    parameter("samId", samordningmelding.samId)
                    parameter("refusjonskrav", samordningmelding.refusjonskrav)
                }

            if (!response.status.isSuccess()) {
                throw ResponseException(response, "Oppdatere samordningsmelding feilet [samId=${samordningmelding.samId}]")
            }
        } catch (e: Exception) {
            logger.error("Oppdatere samordningsmelding feilet [samId=${samordningmelding.samId}]", e)
            throw SamordneVedtakGenerellException("Oppdatere samordningsmelding feilet [samId=${samordningmelding.samId}]", e)
        }
    }
}

internal fun Vedtak.tilSamordneRequest(etterbetaling: Boolean): SamordneVedtakRequest {
    val innhold =
        when (this.innhold) {
            is VedtakInnhold.Behandling -> this.innhold
            is VedtakInnhold.Tilbakekreving, is VedtakInnhold.Klage -> throw SamordneVedtakBehandlingUgyldigForespoerselException(
                "Tilbakekreving skal ikke gjennom samordning",
            )
        }

    return SamordneVedtakRequest(
        pid = this.soeker,
        vedtakId = this.id,
        sakId = this.sakId,
        virkFom = innhold.virkningstidspunkt.atDay(1),
        virkTom =
            innhold.utbetalingsperioder
                .maxByOrNull { it.periode.fom }
                ?.periode
                ?.tom
                ?.atEndOfMonth(),
        fagomrade = "EYO",
        ytelseType = "OMS",
        etterbetaling = etterbetaling,
    )
}

internal data class SamordneVedtakRequest(
    val pid: Folkeregisteridentifikator,
    val vedtakId: Long,
    val sakId: Long,
    val virkFom: LocalDate?,
    val virkTom: LocalDate?,
    val fagomrade: String,
    val ytelseType: String,
    val etterbetaling: Boolean,
)

private class SamordneVedtakRespons(
    val ventPaaSvar: Boolean,
)

class SamordneVedtakBehandlingUgyldigForespoerselException(
    override val message: String,
) : UgyldigForespoerselException(
        "SAMORDNE_VEDTAK_UGYLDIG_FORESPOERSEL",
        message,
    )

class SamordneVedtakGenerellException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)
