package no.nav.etterlatte.grunnlag.aldersovergang

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.time.YearMonth

fun Route.aldersovergangRoutes(aldersovergangService: AldersovergangService) {
    route("/aldersovergang") {
        get("{yearMonth}") {
            val yearMonth = call.parameters["yearMonth"].toString().let { YearMonth.parse(it) }
            call.respond(aldersovergangService.hentSoekereFoedtIEnGittMaaned(yearMonth))
        }
    }
}
