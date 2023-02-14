package no.nav.etterlatte.behandling.omberegning

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
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
                .maxByOrNull { it.behandlingOpprettet }
                ?: throw IllegalArgumentException("Fant ikke forrige behandling i sak ${request.sakId}")
            val omberegning = omberegningService.opprettOmberegning(
                persongalleri = forrigeBehandling.persongalleri,
                sakId = request.sakId,
                aarsak = request.aarsak
            )
            call.respond(omberegning.lagretBehandling.id)
        }
    }
}