package no.nav.etterlatte.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.brev.SamordningManueltBehandletRequest
import no.nav.etterlatte.brev.VedtakTilJournalfoering
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.BrevDistribusjonResponse
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.JournalfoerVedtaksbrevResponseOgBrevid
import no.nav.etterlatte.brev.model.OpprettJournalfoerOgDistribuerRequest
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import java.time.Duration

class BrevapiKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val baseUrl = config.getString("brevapi.resource.url")

    internal suspend fun opprettJournalFoerOgDistribuer(
        sakid: SakId,
        opprett: OpprettJournalfoerOgDistribuerRequest,
    ): BrevDistribusjonResponse {
        try {
            logger.info("Oppretter brev for sak med sakId=$sakid")
            return retryOgPakkUt(times = 5, vent = { timesleft -> Thread.sleep(Duration.ofSeconds(1L * timesleft)) }) {
                httpClient
                    .post("$baseUrl/api/brev/sak/${sakid.sakId}/opprett-journalfoer-og-distribuer") {
                        contentType(ContentType.Application.Json)
                        setBody(opprett.toJson())
                    }.body<BrevDistribusjonResponse>()
            }
        } catch (e: ResponseException) {
            logger.error("Henting av grunnlag for sak med sakId=$sakid feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_OPPRETTELSE_AV_BREV",
                detail = "Kunne ikke opprette brev for sak: $sakid",
            )
        }
    }

    internal suspend fun distribuer(
        brevId: BrevID,
        distribusjonsType: DistribusjonsType,
        sakId: SakId,
    ): BestillingsIdDto {
        try {
            logger.info("Distribuerer brev med id $brevId")
            return retryOgPakkUt(times = 5, vent = { timesleft -> Thread.sleep(Duration.ofSeconds(1L * timesleft)) }) {
                httpClient
                    .post(
                        "$baseUrl/api/brev/$brevId/distribuer?distribusjonsType=${distribusjonsType.name}&sakId=${sakId.sakId}",
                    ) {
                        contentType(ContentType.Application.Json)
                    }.body<BestillingsIdDto>()
            }
        } catch (e: ResponseException) {
            logger.error("Distribuering av brev med brevId=$brevId feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_KAN_IKKE_DISTRIBUERE_BREV",
                detail = "Kunne ikke distribuere brev med id: $brevId",
            )
        }
    }

    internal suspend fun journalfoerVedtaksbrev(vedtakjournalfoering: VedtakTilJournalfoering): JournalfoerVedtaksbrevResponseOgBrevid? {
        val sakId = vedtakjournalfoering.sak.id
        try {
            logger.info("Journalfører brev med sakid: $sakId")
            return retryOgPakkUt(times = 5, vent = { timesleft -> Thread.sleep(Duration.ofSeconds(1L * timesleft)) }) {
                httpClient
                    .post("$baseUrl/api/brev/behandling/${vedtakjournalfoering.behandlingId}/journalfoer-vedtak") {
                        contentType(ContentType.Application.Json)
                        setBody(vedtakjournalfoering.toJson())
                    }.body<JournalfoerVedtaksbrevResponseOgBrevid?>()
            }
        } catch (e: ResponseException) {
            logger.error("Journalføring for brev med sakid=$sakId feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_JOURNALFOERING_AV_BREV",
                detail = "Kunne ikke journalføre brev for sakid: $sakId",
            )
        }
    }

    internal suspend fun opprettOgJournalfoerNotat(
        sakId: SakId,
        samordningManueltBehandletRequest: SamordningManueltBehandletRequest,
    ) {
        try {
            logger.info("Oppretet og journalfører notat med sakid: $sakId")
            retryOgPakkUt(times = 5, vent = { timesleft -> Thread.sleep(Duration.ofSeconds(1L * timesleft)) }) {
                httpClient.post("$baseUrl/api/notat/sak/${sakId.sakId}/manuellsamordning") {
                    contentType(ContentType.Application.Json)
                    setBody(samordningManueltBehandletRequest.toJson())
                }
            }
        } catch (e: ResponseException) {
            logger.error("Opprettelse og journalføring for notat med sakid=$sakId feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_OPPRETT_OG_JOURNALFOERING_AV_NOTAT",
                detail = "Kunne ikke opprette og journalføre notat for sakid: $sakId",
            )
        }
    }
}
