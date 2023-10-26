package no.nav.etterlatte.vedtaksvurdering.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
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
    httpClient: HttpClient,
    private val featureToggleService: FeatureToggleService,
) : SamKlient {
    private val logger = LoggerFactory.getLogger(SamKlient::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("samordnevedtak.client.id")
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
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/vedtak/samordne",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = vedtak.tilSamordneRequest(),
                )
                .mapBoth(
                    success = { json -> json.response.let { objectMapper.readValue<SamordneVedtakRespons>(it.toString()) }.ventPaaSvar },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            logger.error("Samordne vedtak feilet", e)
            throw SamordneVedtakExceptionUnauthorizedException(
                "Samordne vedtak feilet [id=${vedtak.id}]",
            )
        }
    }
}

internal fun Vedtak.tilSamordneRequest(): SamordneVedtakRequest {
    val innhold =
        when (this.innhold) {
            is VedtakBehandlingInnhold -> this.innhold
            is VedtakTilbakekrevingInnhold -> throw SamordneVedtakBehandlingIkkeStoettetException(
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

class SamordneVedtakExceptionUnauthorizedException(override val message: String) : ForespoerselException(
    status = HttpStatusCode.Unauthorized.value,
    code = "SAMORDNE_VEDTAK_FEIL_I_TILGANG",
    detail = message,
)

class SamordneVedtakBehandlingIkkeStoettetException(override val message: String) : UgyldigForespoerselException(
    "SAMORDNE_VEDTAK_BEHANDLING_IKKE_STOETTET",
    message,
)

enum class SamordneVedtakFeatureToggle(private val key: String) : FeatureToggle {
    SamordneVedtakMedSamToggle("pensjon-etterlatte.samordne-vedtak-med-sam"),
    ;

    override fun key() = key
}
