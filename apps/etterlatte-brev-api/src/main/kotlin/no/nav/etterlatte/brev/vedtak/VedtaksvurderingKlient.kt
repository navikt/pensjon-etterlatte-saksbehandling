package no.nav.etterlatte.brev.vedtak

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class VedtakvurderingKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class VedtaksvurderingKlient(config: Config, httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    suspend fun hentVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        try {
            logger.info("Henter vedtaksvurdering behandling med behandlingId=$behandlingId")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/api/vedtak/$behandlingId"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource -> resource.response.let { deserialize(it.toString()) } },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            throw VedtakvurderingKlientException(
                "Henting vedtak for behandling med behandlingId=$behandlingId feilet",
                e,
            )
        }
    }

    suspend fun hentInnvilgelsesdato(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): LocalDate? {
        try {
            logger.info("Henter innvilgelsesdato for sak med id $sakId")

            return downstreamResourceClient.get(
                Resource(clientId, "$resourceUrl/vedtak/$sakId/behandlinger/nyeste/${VedtakType.INNVILGELSE}"),
                brukerTokenInfo,
            ).mapBoth(
                success = { resource ->
                    resource.response?.toString()?.let {
                        val deserialize: VedtakDto? = deserialize(it)
                        deserialize?.vedtakFattet?.tidspunkt?.toLocalDate()
                    }
                },
                failure = { errorResponse -> throw errorResponse },
            )
        } catch (e: Exception) {
            throw VedtakvurderingKlientException(
                "Henting av innvilgelsesdato for sak med id $sakId feilet",
                e,
            )
        }
    }
}
