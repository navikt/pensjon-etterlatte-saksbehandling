package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.brev.tilgangssjekk.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import org.slf4j.LoggerFactory

fun Route.vedtaksbrevRoute(service: VedtaksbrevService, behandlingKlient: BehandlingKlient) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.VedaksbrevRoute")

    route("brev") {
        post("behandling/{$BEHANDLINGSID_CALL_PARAMETER}/vedtak") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val (sakId) = call.receive<OpprettVedtaksbrevRequest>()
                logger.info("Genererer vedtaksbrev for behandling (sakId=$sakId, behandlingId=$behandlingId)")
                val brev = service.oppdaterVedtaksbrev(sakId, behandlingId, bruker)

                call.respond(brev)
            }
        }

        post("behandling/{$BEHANDLINGSID_CALL_PARAMETER}/attestert") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val ferdigstiltOK = service.ferdigstillVedtaksbrev(behandlingId)

                if (ferdigstiltOK) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}

data class OpprettVedtaksbrevRequest(
    val sakId: Long
)