package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

interface TilbakekrevingKlient {
    suspend fun sendTilbakekrevingsvedtak(
        brukerTokenInfo: BrukerTokenInfo,
        tilbakekrevingVedtak: TilbakekrevingVedtak,
    )
}

class TilbakekrevingKlientImpl(config: Config, httpClient: HttpClient) : TilbakekrevingKlient {
    private val logger = LoggerFactory.getLogger(TilbakekrevingKlientImpl::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("tilbakekreving.client.id")
    private val resourceUrl = config.getString("tilbakekreving.resource.url")

    override suspend fun sendTilbakekrevingsvedtak(
        brukerTokenInfo: BrukerTokenInfo,
        tilbakekrevingVedtak: TilbakekrevingVedtak,
    ) {
        try {
            logger.info("Sender tilbakekrevingsvedtak til tilbakekreving med vedtakId=${tilbakekrevingVedtak.vedtakId}")

            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/tilbakekreving/tilbakekrevingsvedtak",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    postBody = tilbakekrevingVedtak,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            throw VedtakKlientException(
                "Lagre tilbakekrevingsvedtak for tilbakekreving med vedtakId=${tilbakekrevingVedtak.vedtakId} feilet",
                e,
            )
        }
    }
}
