package no.nav.etterlatte.no.nav.etterlatte.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.brev.model.BrevDistribusjonResponse
import no.nav.etterlatte.brev.model.OpprettJournalfoerOgDistribuerRequest
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import java.time.Duration

class BrevKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val baseUrl = config.getString("brevapi.resource.url")

    internal suspend fun opprettBrev(
        sakid: SakId,
        opprett: OpprettJournalfoerOgDistribuerRequest,
    ): BrevDistribusjonResponse {
        try {
            logger.info("Oppretter brev uten distribusjon for sak med sakId=$sakid")
            return retryOgPakkUt(times = 5, vent = { timesleft -> Thread.sleep(Duration.ofSeconds(1L * timesleft)) }) {
                httpClient
                    .post("$baseUrl/api/brev/sak/${sakid.sakId}/opprett-for-omregning") {
                        contentType(ContentType.Application.Json)
                        setBody(opprett.toJson())
                    }.body<BrevDistribusjonResponse>()
            }
        } catch (e: ResponseException) {
            logger.error("Opprettelse av brev feilet for sak med sakId=$sakid feilet", e)
            throw InternfeilException("Kunne ikke opprette brev for sak: $sakid")
        }
    }

    internal suspend fun opprettJournalFoerOgDistribuer(
        sakid: SakId,
        opprett: OpprettJournalfoerOgDistribuerRequest,
    ): BrevDistribusjonResponse {
        try {
            logger.info("Oppretter brev uten distribusjon for sak med sakId=$sakid")
            return retryOgPakkUt(times = 5, vent = { timesleft -> Thread.sleep(Duration.ofSeconds(1L * timesleft)) }) {
                httpClient
                    .post("$baseUrl/api/brev/sak/${sakid.sakId}/opprett-journalfoer-og-distribuer-for-omregning") {
                        contentType(ContentType.Application.Json)
                        setBody(opprett.toJson())
                    }.body<BrevDistribusjonResponse>()
            }
        } catch (e: ResponseException) {
            logger.error("Opprettelse av brev feilet for sak med sakId=$sakid feilet", e)
            throw InternfeilException("Kunne ikke opprette brev for sak: $sakid")
        }
    }
}
