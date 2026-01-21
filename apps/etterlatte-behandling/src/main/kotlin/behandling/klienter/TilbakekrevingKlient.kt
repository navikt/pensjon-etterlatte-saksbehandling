package no.nav.etterlatte.behandling.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.feilhaandtering.ExceptionResponse
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tilbakekreving.HentOmgjoeringKravgrunnlagRequest
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TILBAKEKREVING_KOMPONENTEN_FEIL
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingskomponentenFeil
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

    suspend fun hentKravgrunnlag(
        brukerTokenInfo: BrukerTokenInfo,
        sakId: SakId,
        kravgrunnlagId: Long,
    ): Kravgrunnlag?

    suspend fun hentKravgrunnlagOmgjoering(
        kravgrunnlagId: Long,
        sak: Sak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Kravgrunnlag?
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
            val fallbackFeil = TilbakekrevingKlientException(
                "Lagre tilbakekrevingsvedtak for tilbakekreving med vedtakId=${tilbakekrevingVedtak.vedtakId} feilet",
            )

            try {
                val parsetFeil = response.body<ExceptionResponse>()
                if (parsetFeil.code == TILBAKEKREVING_KOMPONENTEN_FEIL) {
                    throw TilbakekrevingskomponentenFeil(parsetFeil.detail)
                } else {
                    throw fallbackFeil
                }
            } catch (e: Exception) {
                logger.warn(
                    "Fikk feil som ikke kunne parses som ExceptionResponse i sending av vedtak " +
                            "${tilbakekrevingVedtak.vedtakId} i sak ${tilbakekrevingVedtak.sakId} til " +
                            "tilbakekrevingskomponenten. Dette burde ikke skje",
                    e
                )
                throw fallbackFeil
            }
        }
    }

    override suspend fun hentKravgrunnlag(
        brukerTokenInfo: BrukerTokenInfo,
        sakId: SakId,
        kravgrunnlagId: Long,
    ): Kravgrunnlag? {
        logger.info("Henter oppdatert kravgrunnlag tilknyttet tilbakekreving for sak $sakId")
        val response =
            client.get("$url/api/tilbakekreving/${sakId.sakId}/kravgrunnlag/$kravgrunnlagId") {
                contentType(ContentType.Application.Json)
            }
        if (response.status == HttpStatusCode.NoContent) {
            return null
        }
        if (!response.status.isSuccess()) {
            throw TilbakekrevingKlientException(
                "Henting av kravgrunnlag tilknyttet tilbakekreving for sak $sakId feilet",
            )
        }

        return response.body<Kravgrunnlag>()
    }

    override suspend fun hentKravgrunnlagOmgjoering(
        kravgrunnlagId: Long,
        sak: Sak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Kravgrunnlag? {
        val response =
            client.post("$url/api/tilbakekreving/${sak.id.sakId}/omgjoering") {
                contentType(ContentType.Application.Json)
                setBody(
                    HentOmgjoeringKravgrunnlagRequest(
                        saksbehandler = brukerTokenInfo.ident(),
                        enhet = sak.enhet,
                        kravgrunnlagId = kravgrunnlagId,
                    ),
                )
            }
        if (response.status == HttpStatusCode.NoContent) {
            return null
        }
        if (!response.status.isSuccess()) {
            throw TilbakekrevingKlientException(
                "Henting av kravgrunnlag for omgjøring av tilbakekreving for sak ${sak.id.sakId} og " +
                        "kravgrunnlagId=$kravgrunnlagId feilet",
            )
        }

        return response.body<Kravgrunnlag>()
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
