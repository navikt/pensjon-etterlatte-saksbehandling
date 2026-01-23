package no.nav.etterlatte.brev.vedtaksbrev

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.JournalfoerBrevService
import no.nav.etterlatte.brev.VedtakTilJournalfoering
import no.nav.etterlatte.brev.model.GenererOgFerdigstillVedtaksbrev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

fun Route.vedtaksbrevRoute(
    service: VedtaksbrevService,
    journalfoerBrevService: JournalfoerBrevService,
    tilgangssjekker: Tilgangssjekker,
) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.VedtaksbrevRoute")

    route("brev/behandling/{$BEHANDLINGID_CALL_PARAMETER}") {
        post("/journalfoer-vedtak") {
            kunSystembruker { systembruker ->
                val vedtak = call.receive<VedtakTilJournalfoering>()
                val journalfoervedtaksbrevResponse =
                    journalfoerBrevService.journalfoerVedtaksbrev(vedtak, systembruker)

                call.respond(journalfoervedtaksbrevResponse ?: HttpStatusCode.NoContent)
            }
        }

        get("vedtak") {
            withBehandlingId(tilgangssjekker) { behandlingId ->
                logger.info("Henter vedtaksbrev for behandling (behandlingId=$behandlingId)")

                measureTimedValue {
                    service.hentVedtaksbrev(behandlingId)
                }.let { (brev, varighet) ->
                    logger.info("Henting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(brev ?: HttpStatusCode.NoContent)
                }
            }
        }

        delete("vedtak") {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                service.settVedtaksbrevTilSlettet(behandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        post("vedtak") {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                val sakId = sakId

                logger.info("Oppretter vedtaksbrev for behandling (sakId=$sakId, behandlingId=$behandlingId)")

                measureTimedValue {
                    service.opprettVedtaksbrev(sakId, behandlingId, brukerTokenInfo)
                }.let { (brev, varighet) ->
                    logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.Created, brev)
                }
            }
        }

        get("vedtak/pdf") {
            withBehandlingId(tilgangssjekker) {
                val brevId =
                    krevIkkeNull(call.request.queryParameters["brevId"]?.toLong()) {
                        "Kan ikke generere PDF uten brevId"
                    }

                logger.info("Genererer PDF for vedtaksbrev (id=$brevId)")

                measureTimedValue {
                    service.genererPdf(brevId, brukerTokenInfo)
                }.let { (pdf, varighet) ->
                    logger.info("Generering av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(pdf)
                }
            }
        }

        post("vedtak/generer-pdf-og-ferdigstill") {
            kunSystembruker { systembruker ->
                val behandlingId = behandlingId
                val request = call.receive<GenererOgFerdigstillVedtaksbrev>()
                logger.info("Generere og ferdigstille vedtaksbrev for behandling (behandlingId=$behandlingId)")

                measureTimedValue {
                    service.genererPdfOgFerdigstill(request.behandlingId, request.brevId, systembruker)
                }.also { (_, varighet) ->
                    logger.info(
                        "Generere og ferdigstilling av vedtaksbrev tok ${
                            varighet.toString(
                                DurationUnit.SECONDS,
                                2,
                            )
                        }",
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        post("vedtak/ferdigstill") {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                logger.info("Ferdigstiller vedtaksbrev for behandling (id=$behandlingId)")

                measureTimedValue {
                    service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo)
                }.also { (_, varighet) ->
                    logger.info("Ferdigstilling av vedtaksbrev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        route("tilbakekreving/fjern-ferdigstilt") {
            post {
                kunSystembruker {
                    withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                        service.fjernFerdigstiltTilbakekrevingbrev(behandlingId)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        put("payload/tilbakestill") {
            withBehandlingId(tilgangssjekker, skrivetilgang = true) {
                val body = call.receive<ResetPayloadRequest>()
                val brevId = body.brevId
                val sakId = body.sakId

                logger.info("Tilbakestiller payload for vedtaksbrev (id=$brevId)")

                measureTimedValue {
                    service.hentNyttInnhold(sakId, brevId, behandlingId, brukerTokenInfo, body.brevtype)
                }.let { (brevPayload, varighet) ->
                    logger.info(
                        "Oppretting av nytt innhold til brev (id=$brevId) tok ${
                            varighet.toString(
                                DurationUnit.SECONDS,
                                2,
                            )
                        }",
                    )
                    call.respond(brevPayload)
                }
            }
        }
    }
}

data class ResetPayloadRequest(
    val brevId: Long,
    val sakId: SakId,
    val brevtype: Brevtype,
)
