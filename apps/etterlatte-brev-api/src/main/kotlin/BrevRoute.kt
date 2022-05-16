package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.db.Mottaker

fun Route.brevRoute(service: BrevService) {
    route("brev") {
        get("maler") {
            val maler = listOf(
                Mal("Vedtak om innvilget barnepensjon", "innvilget"),
                Mal("Revurdert barnepensjon", "revurdering"),
                Mal("Dokumentasjon om vergemål", "verge")
            )

            call.respond(maler)
        }

        get("{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]!!

            call.respond(service.hentAlleBrev(behandlingId))
        }

        post("{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]!!
            val request = call.receive<OpprettBrevRequest>()

            val brev = service.opprett(behandlingId, request.mottaker, request.mal)

            call.respond(brev)
        }

        post("{brevId}/pdf") {
            val brevId = call.parameters["brevId"]!!
            val bytes = service.hentBrevInnhold(brevId.toLong())

            call.respond(bytes)
        }

        delete("{brevId}") {
            val brevId = call.parameters["brevId"]!!

            val brev = service.slettBrev(brevId.toLong())

            if (brev) call.respond("OK")
            else call.respond(HttpStatusCode.BadRequest)
        }

        post("{brevId}/ferdigstill") {
            val brevId = call.parameters["brevId"]!!

            val brev = service.ferdigstillBrev(brevId.toLong())

            call.respond(brev)
        }
    }
}

data class Mal(
    val tittel: String,
    val navn: String
)

data class OpprettBrevRequest(
    val mal: Mal,
    val mottaker: Mottaker
)
