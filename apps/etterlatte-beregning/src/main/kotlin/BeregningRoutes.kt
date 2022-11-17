package nav.no.etterlatte

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.model.BeregningService

fun Route.beregning(beregningService: BeregningService) {
    route("api/beregning") {
        get("{beregningsid}") {
        }
        post("opprett") {
            val beregningsid = beregningService.beregnResultat()
            // sakogbehandlingklient.oppdater(beregningsid)
            return ok
        }
        post("oppdater/{beregningsid}") {
            val beregningsid = beregningService.bekretftberegnetresulat()
            return ok
        }
    }
}