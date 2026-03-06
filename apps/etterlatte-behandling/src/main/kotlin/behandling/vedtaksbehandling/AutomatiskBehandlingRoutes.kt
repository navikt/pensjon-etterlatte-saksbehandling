package no.nav.etterlatte.behandling.vedtaksbehandling

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.migrering.MigreringKjoringVariant
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory

fun Route.automatiskBehandlingRoutes(service: AutomatiskBehandlingService) {
    route("/api/vedtak") {
        val logger = LoggerFactory.getLogger("AutomatiskBehandlingRoute")
        // Automatisk hva da?
        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk") {
            kunSkrivetilgang {
                logger.info("Håndterer behandling $behandlingId")
                val nyttVedtak =
                    service.vedtakStegvis(behandlingId, sakId, brukerTokenInfo, MigreringKjoringVariant.FULL_KJORING)
                call.respond(nyttVedtak)
            }
        }

        post("/{$SAKID_CALL_PARAMETER}/{$BEHANDLINGID_CALL_PARAMETER}/automatisk/stegvis") {
            kunSkrivetilgang {
                val kjoringVariant = call.receive<MigreringKjoringVariant>()
                logger.info("Håndterer behandling $behandlingId med kjøringsvariant ${kjoringVariant.name}")
                val nyttVedtak = service.vedtakStegvis(behandlingId, sakId, brukerTokenInfo, kjoringVariant)
                call.respond(nyttVedtak)
            }
        }
    }
}
