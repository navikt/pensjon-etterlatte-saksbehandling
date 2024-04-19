package no.nav.etterlatte.vedtaksvurdering.simulering

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient

internal fun Route.simuleringOsRoutes(
    service: SimuleringOsService,
    behandlingKlient: BehandlingKlient,
) {
    val logger = application.log

    route("/api/vedtak/simulering/os") {
        route("{$BEHANDLINGID_CALL_PARAMETER}") {
            post {
                withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                    logger.info("Foretar simulering mot Oppdrag for behandling=$behandlingId")
                    val beregning = service.simuler(behandlingId, brukerTokenInfo)
                    call.respond(beregning)
                }
            }
        }
    }
}
