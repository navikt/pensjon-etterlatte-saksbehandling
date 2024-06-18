package no.nav.etterlatte.behandling.klienter

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ping
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

interface TilbakekrevingKlient : Pingable {
    suspend fun sendTilbakekrevingsvedtak(
        brukerTokenInfo: BrukerTokenInfo,
        tilbakekrevingVedtak: TilbakekrevingVedtak,
    )
}

class TilbakekrevingKlientImpl(
    private val client: HttpClient,
    private val url: String,
) : TilbakekrevingKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun sendTilbakekrevingsvedtak(
        brukerTokenInfo: BrukerTokenInfo,
        tilbakekrevingVedtak: TilbakekrevingVedtak,
    ) {
        logger.info("Sender tilbakekrevingsvedtak til tilbakekreving med vedtakId=${tilbakekrevingVedtak.vedtakId}")
        val response =
            client.post("$url/api/tilbakekreving/${tilbakekrevingVedtak.sakId}/vedtak") {
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

    override val serviceName: String
        get() = this.javaClass.simpleName
    override val beskrivelse: String
        get() = "Sender tilbakekrevingsvedtak til tilbakekreving"
    override val endpoint: String
        get() = this.url

    override suspend fun ping(konsument: String?): PingResult =
        client.ping(
            pingUrl = url.plus("/health/isready"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
        )
}

class TilbakekrevingKlientException(
    override val message: String,
) : Exception(message)
