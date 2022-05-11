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

            call.respond(service.hentBrev(behandlingId))
        }

        post("{behandlingId}") {
            val behandlingId = context.parameters["behandlingId"]!!
            val mottaker = call.receive<Mottaker>()

            val brev = service.opprett(behandlingId, mottaker)

            call.respond(brev)
        }

        post("{behandlingId}/pdf") {
            val behandlingId = context.parameters["behandlingId"]!!
//            val request = call.receive<OpprettBrevRequest>()

//            val pdfBytes = service.genererPdf(behandlingId)
//
            call.respond("".toByteArray())
        }

        get("{behandlingId}/send") {
            val behandlingId = context.parameters["behandlingId"]!!

            service.sendBrev(behandlingId)

            call.respond("OK")
        }
    }
}

class OpprettBrevRequest(
    val mottaker: Mottaker
)
