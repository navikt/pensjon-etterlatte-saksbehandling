package no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient

fun Route.automatiskBehandlingRoutes(
    service: AutomatiskBehandlingService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/vedtak") {
        val logger = routeLogger

        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Håndterer behandling $behandlingId")
                val nyttVedtak =
                    service.vedtakStegvis(behandlingId, sakId, brukerTokenInfo, MigreringKjoringVariant.FULL_KJORING)
                call.respond(nyttVedtak)
            }
        }

        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk/stegvis") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val kjoringVariant = call.receive<MigreringKjoringVariant>()
                logger.info("Håndterer behandling $behandlingId med kjøringsvariant ${kjoringVariant.name}")
                val nyttVedtak = service.vedtakStegvis(behandlingId, sakId, brukerTokenInfo, kjoringVariant)
                call.respond(nyttVedtak)
            }
        }
    }
}
