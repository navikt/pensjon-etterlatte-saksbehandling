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
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

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
                    logger.info("Oppretter vedtaksbrev for tilbakekreving behandling (behandlingId=$behandlingId)")
                    val brev = service.opprettVedtaksbrev(behandlingId, brukerTokenInfo, request)
                    call.respond(HttpStatusCode.Created, brev)
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
                    val pdf = service.genererPdf(brevId, brukerTokenInfo, request)
                    call.respond(pdf)
                }
            }

            post("ferdigstill") {
                withBehandlingId(tilgangssjekker, skrivetilgang = true) { behandlingId ->
                    logger.info("Ferdigstiller vedtaksbrev for behandling (id=$behandlingId)")
                    service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
