package no.nav.etterlatte.behandling.revurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.GenerellBehandlingService
import no.nav.etterlatte.behandling.domain.toDetaljertBehandling
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sakId

internal fun Route.revurderingRoutes(
    revurderingService: RevurderingService,
    generellBehandlingService: GenerellBehandlingService
) {
    val logger = application.log

    route("/api/revurdering/{$SAKID_CALL_PARAMETER}") {
        post {
            logger.info("Oppretter ny revurdering på sak $sakId")
            val body = try {
                call.receive<OpprettRevurderingRequest>()
            } catch (e: Exception) {
                logger.error("Feil skjedde under lesing av payloaden.", e)
                call.respond(HttpStatusCode.BadRequest, "Feil under deserialiseringen av objektet")
                return@post
            }
            if (!body.aarsak.kanBrukesIMiljo()) {
                call.respond(HttpStatusCode.BadRequest, "Feil revurderingsårsak, foreløpig ikke støttet")
                return@post
            }
            generellBehandlingService.hentSisteIverksatte(sakId)?.let { forrigeIverksatteBehandling ->
                val sakType = forrigeIverksatteBehandling.sak.sakType
                if (!body.aarsak.gyldigForSakType(sakType)) {
                    call.respond(HttpStatusCode.BadRequest, "${body.aarsak} er ikke støttet for $sakType")
                    return@post
                }

                val revurdering = revurderingService.opprettManuellRevurdering(
                    sakId = forrigeIverksatteBehandling.sak.id,
                    forrigeBehandling = forrigeIverksatteBehandling,
                    revurderingAarsak = body.aarsak,
                    kilde = Vedtaksloesning.GJENNY
                )

                when (revurdering) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(revurdering.toDetaljertBehandling())
                }
            } ?: call.respond(HttpStatusCode.BadRequest, "Kan ikke revurdere en sak uten iverksatt behandling")
        }
    }

    route("/api/stoettederevurderinger/{saktype}") {
        get {
            val sakType = call.parameters["saktype"]?.let { runCatching { SakType.valueOf(it) }.getOrNull() }
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig saktype")

            val stoettedeRevurderinger = RevurderingAarsak.values()
                .filter { it.kanBrukesIMiljo() && it.gyldigForSakType(sakType) }
                .map { it.name }
            call.respond(stoettedeRevurderinger)
        }
    }
}

data class OpprettRevurderingRequest(val aarsak: RevurderingAarsak)