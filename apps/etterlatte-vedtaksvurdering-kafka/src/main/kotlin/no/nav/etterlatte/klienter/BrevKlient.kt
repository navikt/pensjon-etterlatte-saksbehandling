package no.nav.etterlatte.no.nav.etterlatte.klienter

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.GenererOgFerdigstillVedtaksbrev
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

class BrevKlient(
    private val httpClient: HttpClient,
    private val url: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    internal fun opprettBrev(
        behandlingId: UUID,
        sakId: SakId,
    ): Brev =
        runBlocking {
            try {
                logger.info("Ber brev-api om å opprette vedtaksbrev for behandling id=$behandlingId")
                retryOgPakkUt(times = 5, vent = { timesleft -> Thread.sleep(Duration.ofSeconds(1L * timesleft)) }) {
                    httpClient
                        .post("$url/api/brev/behandling/$behandlingId/vedtak?sakId=${sakId.sakId}") {
                            contentType(ContentType.Application.Json)
                            // setBody(opprett.toJson())
                        }.body<Brev>()
                }
            } catch (e: ResponseException) {
                logger.error("Opprettelse av brev feilet for behandling med id=$behandlingId", e)
                throw InternfeilException("Opprettelse av brev feilet for behandling med id=$behandlingId")
            }
        }

    internal fun genererPdfOgFerdigstillVedtaksbrev(
        behandlingId: UUID,
        request: GenererOgFerdigstillVedtaksbrev,
    ) = runBlocking {
        try {
            logger.info("Kaller brev-api for å generere og ferdigstille vedtaksbrev for $behandlingId")
            retryOgPakkUt(times = 5, vent = { timesleft -> Thread.sleep(Duration.ofSeconds(1L * timesleft)) }) {
                httpClient
                    .post("$url/api/brev/behandling/$behandlingId/vedtak/generer-pdf-og-ferdigstill") {
                        contentType(ContentType.Application.Json)
                        setBody(request.toJson())
                    }
            }
        } catch (e: ResponseException) {
            logger.error(
                "Forsøk på å generere pdf og ferdigstille vedtaksbrev feilet for behandling med id=$behandlingId",
                e,
            )
            throw InternfeilException("Forsøk på å generere pdf og ferdigstille vedtaksbrev feilet for behandling med id=$behandlingId")
        }
    }
}
