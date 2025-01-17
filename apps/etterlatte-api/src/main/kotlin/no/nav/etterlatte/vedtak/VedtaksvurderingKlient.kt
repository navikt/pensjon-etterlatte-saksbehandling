package no.nav.etterlatte.vedtak

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.vedtak.VedtakForEksterntDto
import no.nav.etterlatte.libs.common.vedtak.VedtakForPersonRequest
import org.slf4j.LoggerFactory

class VedtaksvurderingKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val vedtaksvurderingUrl = "${config.getString("vedtak.url")}/api/vedtak/for/eksternt"

    suspend fun hentVedtak(request: VedtakForPersonRequest): VedtakForEksterntDto {
        sikkerlogger().info("Henter vedtak med fnr=${request.fnr}")

        return try {
            httpClient
                .post {
                    url(vedtaksvurderingUrl)
                    setBody(request)
                }.body()
        } catch (e: ClientRequestException) {
            logger.error("Det oppstod feil i kall til vedtak API", e)
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw IkkeTillattException("VEDTAK-TILGANG", "Vedtak: Ikke tilgang")
                HttpStatusCode.BadRequest -> throw UgyldigForespoerselException(
                    "VEDTAK-FORESPOERSEL",
                    "Vedtak: Ugyldig forespørsel",
                )

                HttpStatusCode.NotFound -> throw IkkeFunnetException(
                    "VEDTAK-IKKE-FUNNET",
                    "Vedtak: Ressurs ikke funnet",
                )

                else -> throw InternfeilException("Intern feil ved uthenting av vedtak")
            }
        }
    }
}
