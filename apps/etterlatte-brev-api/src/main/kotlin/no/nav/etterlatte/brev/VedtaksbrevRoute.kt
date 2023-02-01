package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.saksbehandler
import org.slf4j.LoggerFactory

fun Route.vedtaksbrevRoute(service: VedtaksbrevService) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.VedaksbrevRoute")

    route("brev") {
        post("vedtak") {
            val (sakId, behandlingId) = call.receive<OpprettVedtaksbrevRequest>()

            logger.info("Genererer vedtaksbrev for behandling (sakId=$sakId, behandlingId=$behandlingId)")
            val brev = service.oppdaterVedtaksbrev(sakId, behandlingId, saksbehandler.ident, getAccessToken(call))

            call.respond(brev)
        }

        post("attestert/{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]!!

            val ferdigstiltOK = service.ferdigstillVedtaksbrev(behandlingId)

            if (ferdigstiltOK) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}

data class OpprettVedtaksbrevRequest(
    val sakId: Long,
    val behandlingId: String
)