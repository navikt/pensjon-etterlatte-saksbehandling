package no.nav.etterlatte.utbetaling.klienter

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksvurderingKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    internal suspend fun hentVedtakSimulering(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        try {
            logger.info("Henter vedtak for behandlingId=$behandlingId")

            return downstreamResourceClient
                .post(
                    Resource(clientId, "$resourceUrl/api/vedtak/$behandlingId/simulering"),
                    brukerTokenInfo,
                    {},
                ).mapBoth(
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (re: ResponseException) {
            logger.error("Ukjent feil ved henting av vedtak for behandling=$behandlingId", re)

            throw ForespoerselException(
                status = re.response.status.value,
                code = "UKJENT_FEIL_HENTING_AV_VEDTAKSVURDERING",
                detail = "Ukjent feil oppsto ved henting av vedtak for behandling",
                meta = mapOf("behandlingId" to behandlingId),
            )
        }
    }
}
