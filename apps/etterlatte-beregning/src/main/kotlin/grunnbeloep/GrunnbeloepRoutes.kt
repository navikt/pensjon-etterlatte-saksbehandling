package no.nav.etterlatte.grunnbeloep

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.time.YearMonth

fun Route.grunnbeloep(service: GrunnbeloepService) {
    route("/api/beregning/grunnbeloep") {
        get {
            call.respond(service.hentGrunnbeloep(YearMonth.now()))
        }
    }
}
