package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.db.Mottaker

fun Route.brevRoute(service: BrevService) {
    route("brev") {
        get("{behandlingId}") {
            val behandlingId = context.parameters["behandlingId"]!!

            call.respond(service.hentAlleBrev(behandlingId))
        }

        post("{behandlingId}") {
            val behandlingId = context.parameters["behandlingId"]!!
            val request = call.receive<OpprettBrevRequest>()

            val brev = service.opprett(behandlingId, request.mottaker, request.mal)

            call.respond(brev)
        }

        post("{brevId}/pdf") {
            val brevId = context.parameters["brevId"]!!
            val bytes = service.hentBrevInnhold(brevId.toLong())

            call.respond(bytes)
        }

        post("{brevId}/ferdigstill") {
            val brevId = context.parameters["brevId"]!!

            val brev = service.ferdigstillBrev(brevId.toLong())

            //TODO: Sending av brev
//            service.sendBrev(behandlingId)

            call.respond(brev)
        }
    }
}

class OpprettBrevRequest(
    val mal: String,
    val mottaker: Mottaker
)
