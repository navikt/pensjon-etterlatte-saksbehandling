package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VedtaksvurderingKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val vedtaksvurderingUrl = "${config.getString("vedtak.url")}/api/samordning/vedtak"

    suspend fun hentVedtak(
        vedtakId: Long,
        callerContext: CallerContext,
    ): VedtakSamordningDto {
        logger.info("Henter vedtaksvurdering med vedtakId=$vedtakId")

        return try {
            httpClient
                .get {
                    url("$vedtaksvurderingUrl/$vedtakId")
                    if (callerContext is MaskinportenTpContext) {
                        header("orgnr", callerContext.organisasjonsnr)
                    }
                }.body()
        } catch (e: ClientRequestException) {
            logger.error("Det oppstod feil i kall til vedtak API", e)
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw VedtakvurderingManglendeTilgangException("Vedtak: Ikke tilgang")
                HttpStatusCode.BadRequest -> throw VedtakvurderingUgyldigForesporselException("Vedtak: Ugyldig forespørsel")
                HttpStatusCode.NotFound -> throw VedtakvurderingIkkeFunnetException("Vedtak: Ressurs ikke funnet")
                else -> throw e
            }
        }
    }

    suspend fun hentVedtaksliste(
        sakType: SakType,
        fomDato: LocalDate,
        fnr: String,
        callerContext: CallerContext,
    ): List<VedtakSamordningDto> {
        logger.info("Henter vedtaksliste, fomDato=$fomDato")

        return try {
            httpClient
                .get(vedtaksvurderingUrl) {
                    parameter("sakstype", sakType)
                    parameter("fomDato", fomDato)
                    header("fnr", fnr)
                    if (callerContext is MaskinportenTpContext) {
                        header("orgnr", callerContext.organisasjonsnr)
                    }
                }.body()
        } catch (e: ClientRequestException) {
            logger.error("Det oppstod feil i kall til vedtaksliste API", e)
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw VedtakvurderingManglendeTilgangException("Vedtak: Ikke tilgang")
                HttpStatusCode.BadRequest -> throw VedtakvurderingUgyldigForesporselException("Vedtak: Ugyldig forespørsel")
                HttpStatusCode.NotFound -> throw VedtakvurderingIkkeFunnetException("Vedtak: Ressurs ikke funnet")
                else -> throw e
            }
        }
    }
}

class VedtakvurderingManglendeTilgangException(
    detail: String,
) : IkkeTillattException(
        code = "020-VEDTAK-TILGANG",
        detail = detail,
        meta = getMeta(),
    )

class VedtakvurderingUgyldigForesporselException(
    detail: String,
) : UgyldigForespoerselException(
        code = "020-VEDTAK-FORESPOERSEL",
        detail = detail,
        meta = getMeta(),
    )

class VedtakvurderingIkkeFunnetException(
    detail: String,
) : IkkeFunnetException(
        code = "020-VEDTAK-IKKE-FUNNET",
        detail = detail,
        meta = getMeta(),
    )
