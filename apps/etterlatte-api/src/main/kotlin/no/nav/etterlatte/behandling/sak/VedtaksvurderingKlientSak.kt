package no.nav.etterlatte.behandling.sak

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VedtaksvurderingKlientSak(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlientSak::class.java)

    private val vedtaksvurderingUrl = "${config.getString("vedtak.url")}/api/vedtak"

    suspend fun hentLoependeVedtak(sakId: SakId): LoependeYtelseDTO {
        val date = LocalDate.now()
        logger.info("Henter lopende vedtak, date=$date")
        return try {
            httpClient
                .get("$vedtaksvurderingUrl/loepende/${sakId.sakId}?dato=$date")
                .body()
        } catch (e: ClientRequestException) {
            logger.error("Det oppstod feil i kall til lopende vedtak API", e)
            when (e.response.status) { // TODO: Håndter dette på en annen måte
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
    )

class VedtakvurderingUgyldigForesporselException(
    detail: String,
) : UgyldigForespoerselException(
        code = "020-VEDTAK-FORESPOERSEL",
        detail = detail,
    )

class VedtakvurderingIkkeFunnetException(
    detail: String,
) : IkkeFunnetException(
        code = "020-VEDTAK-IKKE-FUNNET",
        detail = detail,
    )
