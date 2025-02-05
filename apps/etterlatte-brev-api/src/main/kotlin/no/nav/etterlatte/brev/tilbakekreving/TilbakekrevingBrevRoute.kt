package no.nav.etterlatte.brev.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

fun Route.tilbakekrevingBrevRoute(
    // service: TilbakekrevingVedtaksbrevService,
    service: SkalTilBehandling, // TODO
    tilgangssjekker: Tilgangssjekker,
) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.tilbakekreving.TilbakekrevingBrevRoute")
    route("brev/tilbakekreving/{$BEHANDLINGID_CALL_PARAMETER}") {
        route("vedtak") {
            post {
                withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                    val sakId = sakId

                    logger.info("Oppretter vedtaksbrev for tilbakekreving behandling (sakId=$sakId, behandlingId=$behandlingId)")

                    measureTimedValue {
                        service.opprettVedtaksbrev(sakId, behandlingId, brukerTokenInfo)
                    }.let { (brev, varighet) ->
                        logger.info("Oppretting av brev tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                        call.respond(HttpStatusCode.Created, brev)
                    }
                }
            }

            get("pdf") {
                withBehandlingId(tilgangssjekker) {
                    val brevId =
                        krevIkkeNull(call.request.queryParameters["brevId"]?.toLong()) {
                            "Kan ikke generere PDF uten brevId"
                        }

                    logger.info("Genererer PDF for tilbakekreving vedtaksbrev (id=$brevId)")

                    measureTimedValue {
                        service.genererPdf(brevId, brukerTokenInfo).bytes
                    }.let { (pdf, varighet) ->
                        logger.info("Generering av pdf tok ${varighet.toString(DurationUnit.SECONDS, 2)}")
                        call.respond(pdf)
                    }
                }
            }
        }
    }
}
