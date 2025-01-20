package no.nav.etterlatte.behandling.sak

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.samordning.vedtak.VedtakvurderingIkkeFunnetException
import no.nav.etterlatte.samordning.vedtak.VedtakvurderingManglendeTilgangException
import no.nav.etterlatte.samordning.vedtak.VedtakvurderingUgyldigForesporselException
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VedtaksvurderingSakKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingSakKlient::class.java)

    private val vedtaksvurderingUrl = "${config.getString("vedtak.url")}/api/vedtak"

    suspend fun hentLoependeVedtak(sakId: SakId): LoependeYtelseDTO {
        val dato = LocalDate.now()
        logger.info("Henter lopende vedtak, date=$dato")
        return try {
            httpClient
                .get("$vedtaksvurderingUrl/loepende/${sakId.sakId}?dato=$dato")
                .body()
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
}
