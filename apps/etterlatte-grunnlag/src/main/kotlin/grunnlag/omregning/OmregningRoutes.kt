package no.nav.etterlatte.grunnlag.omregning

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.medBody
import java.time.YearMonth

fun Route.omregningRoutes(omregningService: OmregningService) {
    route("/omregning") {
        get {
            medBody<YearMonth> {
                call.respond(omregningService.hentSoekereFoedtIEnGittMaaned(it))
            }
        }
    }
}
