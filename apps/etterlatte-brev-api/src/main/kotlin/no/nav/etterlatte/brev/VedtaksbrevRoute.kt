package no.nav.etterlatte.brev

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import org.slf4j.LoggerFactory
import java.util.*

fun Route.vedtaksbrevRoute(service: VedtaksbrevService) {
    val logger = LoggerFactory.getLogger("no.nav.etterlatte.brev.VedaksbrevRoute")

    route("brev") {
        post("vedtak") {
            val (sakId, behandlingId) = call.receive<OpprettVedtaksbrevRequest>()

            logger.info("Genererer vedtaksbrev for behandling (sakId=$sakId, behandlingId=$behandlingId)")
            val brev = service.oppdaterVedtaksbrev(sakId, behandlingId, bruker)

            call.respond(brev)
        }

        post("attestert/{behandlingId}") {
            withBehandlingId { behandlingId ->
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
    val sakId: Long,
    val behandlingId: UUID
)