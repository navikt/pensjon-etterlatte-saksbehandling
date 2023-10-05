package no.nav.etterlatte.samordning.vedtak

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VedtakvurderingKlientException(message: String, cause: Throwable) :
    Exception(message, cause)

class VedtaksvurderingKlient(config: Config, private val httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val vedtaksvurderingUrl = "${config.getString("vedtak.url")}/api/samordning/vedtak"

    suspend fun hentVedtak(
        vedtakId: Long,
        organisasjonsnummer: String,
    ): VedtakSamordningDto {
        logger.info("Henter vedtaksvurdering med vedtakId=$vedtakId")

        return try {
            httpClient.get {
                url("$vedtaksvurderingUrl/$vedtakId")
                header("orgnr", organisasjonsnummer)
            }.body()
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw VedtakvurderingKlientException("Vedtak: Ikke tilgang", e)
                HttpStatusCode.BadRequest -> throw VedtakvurderingKlientException("Vedtak: Ugyldig forespørsel", e)
                HttpStatusCode.NotFound -> throw VedtakvurderingKlientException("Vedtak: Ressurs ikke funnet", e)
                else -> throw e
            }
        }
    }

    suspend fun hentVedtaksliste(
        virkFom: LocalDate,
        fnr: String,
        organisasjonsnummer: String,
    ): List<VedtakSamordningDto> {
        logger.info("Henter vedtaksliste, virkFom=$virkFom")

        return try {
            httpClient.get {
                url("$vedtaksvurderingUrl?virkFom=$virkFom")
                header("fnr", fnr)
                header("orgnr", organisasjonsnummer)
            }.body()
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw VedtakvurderingKlientException("Vedtak: Ikke tilgang", e)
                HttpStatusCode.BadRequest -> throw VedtakvurderingKlientException("Vedtak: Ugyldig forespørsel", e)
                HttpStatusCode.NotFound -> throw VedtakvurderingKlientException("Vedtak: Ressurs ikke funnet", e)
                else -> throw e
            }
        }
    }
}
