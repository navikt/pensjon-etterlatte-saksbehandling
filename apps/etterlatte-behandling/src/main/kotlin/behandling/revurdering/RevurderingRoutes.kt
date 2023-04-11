package no.nav.etterlatte.behandling.revurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.sakId

internal fun Route.revurderingRoutes(
    revurderingService: RevurderingService,
    generellBehandlingService: GenerellBehandlingService
) {
    val logger = application.log

    route("/api/{$SAKID_CALL_PARAMETER}/revurdering") {
        post {
            logger.info("Oppretter ny revurdering på sak $sakId")
            val body = try {
                call.receive<OpprettRevurderingRequest>()
            } catch (e: Exception) {
                logger.error("Feil skjedde under lesing av payloaden. Mangler gitt revurderingsårsak.")
                call.respond(HttpStatusCode.BadRequest, "Støtter ikke denne revurderingstypen")
                return@post
            }

            generellBehandlingService.hentSenestIverksatteBehandling(sakId)?.let { forrigeIverksatteBehandling ->
                val revurdering = revurderingService.opprettManuellRevurdering(
                    sakId = forrigeIverksatteBehandling.sak.id,
                    forrigeBehandling = forrigeIverksatteBehandling,
                    revurderingAarsak = body.aarsak,
                    kilde = Vedtaksloesning.DOFFEN
                )

                when (revurdering) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(revurdering.toDetaljertBehandling())
                }
            } ?: call.respond(HttpStatusCode.BadRequest, "Kan ikke revurdere en sak uten iverksatt behandling")
        }
    }
}

data class OpprettRevurderingRequest(val aarsak: RevurderingAarsak)