package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import model.FattetVedtak
import model.VedtakType
import java.time.LocalDate

fun Route.brevRoute(service: BrevService) {
    route("brev") {
        get("opprett") {

            call.respond(service.opprettBrev(FattetVedtak(VedtakType.INNVILGELSE, LocalDate.now(), LocalDate.now(), 2500.00)))
        }
    }
}
