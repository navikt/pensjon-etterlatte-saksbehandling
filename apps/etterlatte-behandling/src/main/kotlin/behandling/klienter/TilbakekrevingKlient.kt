package no.nav.etterlatte.behandling.klienter

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

interface TilbakekrevingKlient {
    suspend fun sendTilbakekrevingsvedtak(
        brukerTokenInfo: BrukerTokenInfo,
        tilbakekrevingVedtak: TilbakekrevingVedtak,
    )
}

class TilbakekrevingKlientImpl(private val httpClient: HttpClient, private val resourceUrl: String) :
    TilbakekrevingKlient {
    private val logger = LoggerFactory.getLogger(TilbakekrevingKlientImpl::class.java)

    override suspend fun sendTilbakekrevingsvedtak(
        brukerTokenInfo: BrukerTokenInfo,
        tilbakekrevingVedtak: TilbakekrevingVedtak,
    ) {
        logger.info("Sender tilbakekrevingsvedtak til tilbakekreving med vedtakId=${tilbakekrevingVedtak.vedtakId}")
        val response =
            httpClient.post("$resourceUrl/api/tilbakekreving/tilbakekrevingsvedtak") {
                contentType(ContentType.Application.Json)
                setBody(
                    tilbakekrevingVedtak,
                )
            }
        if (!response.status.isSuccess()) {
            throw TilbakekrevingKlientException(
                "Lagre tilbakekrevingsvedtak for tilbakekreving med vedtakId=${tilbakekrevingVedtak.vedtakId} feilet",
            )
        }
    }
}

class TilbakekrevingKlientException(override val message: String) : Exception(message)
