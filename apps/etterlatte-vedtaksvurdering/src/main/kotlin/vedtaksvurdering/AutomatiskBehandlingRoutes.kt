package no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringKjoringVariant
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient

fun Route.automatiskBehandlingRoutes(
    service: AutomatiskBehandlingService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/vedtak") {
        val logger = application.log

        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Håndterer behandling $behandlingId")
                val nyttVedtak =
                    service.vedtakStegvis(behandlingId, sakId, brukerTokenInfo, MigreringKjoringVariant.FULL_KJORING)
                call.respond(nyttVedtak.toDto())
            }
        }

        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk/stegvis") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val kjoringVariant = call.receive<MigreringKjoringVariant>()
                logger.info("Håndterer behandling $behandlingId med kjøringsvariant ${kjoringVariant.name}")
                val nyttVedtak = service.vedtakStegvis(behandlingId, sakId, brukerTokenInfo, kjoringVariant)
                call.respond(nyttVedtak.toNyDto())
            }
        }
    }
}
