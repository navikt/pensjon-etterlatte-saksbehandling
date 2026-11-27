package no.nav.etterlatte.soeknadsmottak

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.soeknadsmottak.JournalfoeringskontekstRequest
import no.nav.etterlatte.libs.common.soeknadsmottak.JournalfoeringskontekstResponse
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringRequest
import no.nav.etterlatte.libs.common.soeknadsmottak.SoeknadOverleveringResponse
import org.slf4j.LoggerFactory

/** Videreformidler søknadsmottaket til etterlatte-behandling, som eier faglogikken og dedupliseringen. */
class SoeknadsmottakKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val behandlingUrl = "${config.getString("behandling.url")}/api/soeknadsmottak"

    suspend fun journalfoeringskontekst(request: JournalfoeringskontekstRequest): JournalfoeringskontekstResponse =
        post("journalfoeringskontekst", request)

    suspend fun overlever(request: SoeknadOverleveringRequest): SoeknadOverleveringResponse = post("overlevering", request)

    private suspend inline fun <reified T> post(
        sti: String,
        body: Any,
    ): T =
        try {
            httpClient
                .post("$behandlingUrl/$sti") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body<T>()
        } catch (e: ResponseException) {
            // Statusen videreformidles, slik at innsendt-soeknad skiller avvist kontrakt (4xx) fra utilgjengelighet (5xx).
            logger.warn("Søknadsmottak $sti i behandling svarte ${e.response.status.value}")
            throw ForespoerselException(
                status = e.response.status.value,
                code = "SOEKNADSMOTTAK_BEHANDLING_FEIL",
                detail = "Søknadsmottaket i behandling svarte ${e.response.status.value}",
                cause = e,
            )
        }
}
