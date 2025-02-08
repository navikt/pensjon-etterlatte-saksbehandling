package no.nav.etterlatte.brev.vedtaksbrev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

fun Route.vedtaksbrevRouteNy(
    service: VedtaksbrevServiceNy,
    tilgangssjekker: Tilgangssjekker,
) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.tilbakekreving.VedtaksbrevRouteNy")
    route("brev/tilbakekreving/{$BEHANDLINGID_CALL_PARAMETER}") {
        route("vedtak") {
            post {
                withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                    val request = call.receive<BrevRequest>()

                    logger.info("Oppretter vedtaksbrev for tilbakekreving behandling (sakId=$sakId, behandlingId=$behandlingId)")

                    measureTimedValue {
                        service.opprettVedtaksbrev(behandlingId, brukerTokenInfo, request)
                    }.let { (brev, varighet) ->
                        logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                        call.respond(HttpStatusCode.Created, brev)
                    }
                }
            }

            post("pdf") {
                withBehandlingId(tilgangssjekker) {
                    val brevId =
                        krevIkkeNull(call.request.queryParameters["brevId"]?.toLong()) {
                            "Kan ikke generere PDF uten brevId"
                        }
                    val request = call.receive<BrevRequest>()

                    logger.info("Genererer PDF for tilbakekreving vedtaksbrev (id=$brevId)")

                    measureTimedValue {
                        service.genererPdf(brevId, brukerTokenInfo, request)
                    }.let { (pdf, varighet) ->
                        logger.info("Generering av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                        call.respond(pdf)
                    }
                }
            }
        }
    }
}
