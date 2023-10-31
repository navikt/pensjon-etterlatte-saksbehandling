package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakBehandlingInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtakTilbakekrevingInnhold
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface SamKlient {
    /**
     * @return true=vente på samordning med tjenestepensjon, false=ikke vente
     */
    suspend fun samordneVedtak(
        vedtak: Vedtak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean
}

class SamKlientImpl(
    config: Config,
    private val httpClient: HttpClient,
    private val featureToggleService: FeatureToggleService,
) : SamKlient {
    private val logger = LoggerFactory.getLogger(SamKlient::class.java)

    private val resourceUrl = config.getString("samordnevedtak.resource.url")

    override suspend fun samordneVedtak(
        vedtak: Vedtak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        if (!featureToggleService.isEnabled(
                toggleId = SamordneVedtakFeatureToggle.SamordneVedtakMedSamToggle,
                defaultValue = false,
            )
        ) {
            return false
        }

        try {
            val response =
                httpClient.post(resourceUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(vedtak.tilSamordneRequest())
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
}

internal fun Vedtak.tilSamordneRequest(): SamordneVedtakRequest {
    val innhold =
        when (this.innhold) {
            is VedtakBehandlingInnhold -> this.innhold
            is VedtakTilbakekrevingInnhold -> throw SamordneVedtakBehandlingUgyldigForespoerselException(
                "Tilbakekreving skal ikke gjennom samordning",
            )
        }

    return SamordneVedtakRequest(
        pid = this.soeker,
        vedtakId = this.id,
        virkFom = innhold.virkningstidspunkt.atDay(1),
        virkTom = innhold.utbetalingsperioder.maxByOrNull { it.periode.fom }?.periode?.tom?.atEndOfMonth(),
        fagomrade = "EYO",
        ytelseType = "OMS",
    )
}

internal data class SamordneVedtakRequest(
    val pid: Folkeregisteridentifikator,
    val vedtakId: Long,
    val virkFom: LocalDate?,
    val virkTom: LocalDate?,
    val fagomrade: String,
    val ytelseType: String,
)

private class SamordneVedtakRespons(val ventPaaSvar: Boolean)

class SamordneVedtakBehandlingUgyldigForespoerselException(override val message: String) : UgyldigForespoerselException(
    "SAMORDNE_VEDTAK_UGYLDIG_FORESPOERSEL",
    message,
)

class SamordneVedtakGenerellException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

enum class SamordneVedtakFeatureToggle(private val key: String) : FeatureToggle {
    SamordneVedtakMedSamToggle("pensjon-etterlatte.samordne-vedtak-med-sam"),
    ;

    override fun key() = key
}
