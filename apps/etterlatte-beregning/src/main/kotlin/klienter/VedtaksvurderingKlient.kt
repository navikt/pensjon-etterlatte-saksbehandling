package no.nav.etterlatte.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

interface VedtaksvurderingKlient {
    suspend fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto>
}

class VedtaksvurderingKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class VedtaksvurderingKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : VedtaksvurderingKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtaksvurdering.client.id")
    private val resourceUrl = config.getString("vedtaksvurdering.resource.url")

    override suspend fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto> {
        logger.info("Henter iverksatte vedtak for sak=$sakId")
        return retry<List<VedtakSammendragDto>> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/vedtak/sak/$sakId/iverksatte",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> objectMapper.readValue(resource.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw VedtaksvurderingKlientException(
                        "Klarte ikke hente iverksatte vedtak for sak=$sakId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }
}
