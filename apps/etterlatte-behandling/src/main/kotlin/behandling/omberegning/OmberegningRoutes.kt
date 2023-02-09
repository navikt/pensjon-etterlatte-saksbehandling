package no.nav.etterlatte.behandling.omberegning

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse

fun Route.omberegningRoutes(
    behandlingService: GenerellBehandlingService,
    omberegningService: OmberegningService
) {
    route("/omberegning") {
        post {
            val request = call.receive<Omberegningshendelse>()
            val forrigeBehandling = behandlingService.hentBehandlingerISak(request.sakId)
                .maxByOrNull { it.behandlingOpprettet }!!
            val omberegning = omberegningService.opprettOmberegning(
                persongalleri = forrigeBehandling.persongalleri,
                sakId = request.sakId,
                aarsak = request.aarsak
            )
            call.respondText { omberegning.lagretBehandling.id.toString() }
        }
    }
}