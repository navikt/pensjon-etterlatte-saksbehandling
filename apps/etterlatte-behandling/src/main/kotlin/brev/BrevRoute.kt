package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

fun Route.brevRoute(service: BrevService) {
    val logger = LoggerFactory.getLogger("BrevRoute")

    route("api/behandling/brev/{$BEHANDLINGID_CALL_PARAMETER}/vedtak") {
        post {
            // TODO tilgangsjekk?
            logger.info("Oppretter vedtaksbrev for tilbakekreving behandling (sakId=$sakId, behandlingId=$behandlingId)")

            measureTimedValue {
                service.opprettVedtaksbrev(behandlingId, sakId, brukerTokenInfo)
            }.let { (brev, varighet) ->
                logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                call.respond(HttpStatusCode.Created, brev)
            }
        }

        get("pdf") {
            // TODO tilgangsjekk?
            val brevId =
                krevIkkeNull(call.request.queryParameters["brevId"]?.toLong()) {
                    "Kan ikke generere PDF uten brevId"
                }
            logger.info("Genererer PDF for tilbakekreving vedtaksbrev (id=$brevId)")

            measureTimedValue {
                service.genererPdf(brevId, behandlingId, sakId, brukerTokenInfo).bytes
            }.let { (pdf, varighet) ->
                logger.info("Generering av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                call.respond(pdf)
            }
        }

        post("ferdigstill") {
            // TODO tilgangsjekk?
            logger.info("Ferdigstiller vedtaksbrev for behandling (id=$behandlingId)")
            measureTimedValue {
                service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo)
            }.also { (_, varighet) ->
                logger.info("Ferdigstilling av vedtaksbrev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                call.respond(HttpStatusCode.OK)
            }
        }

        put("tilbakestill") {
            // TODO tilgangsjekk?
            val brevId =
                krevIkkeNull(call.request.queryParameters["brevId"]?.toLong()) {
                    "Kan ikke tilbaketille PDF uten brevId"
                }
            logger.info("Tilbakestiller payload for vedtaksbrev (id=$brevId)")

            measureTimedValue {
                service.tilbakestillVedtaksbrev(brevId, behandlingId, sakId, brukerTokenInfo)
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
