package no.nav.etterlatte.vilkaarsvurdering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.vilkaarsvurdering.service.AldersovergangService
import java.util.UUID

fun Route.aldersovergang(aldersovergangService: AldersovergangService) {
    route("/api/vilkaarsvurdering/aldersovergang") {
        val logger = routeLogger

        post("/{$BEHANDLINGID_CALL_PARAMETER}") {
            val loependeBehandlingId =
                call.request.queryParameters["fra"]?.let { UUID.fromString(it) }
                    ?: throw IllegalArgumentException("Mangler query parameter 'fra'")

            logger.info("Behandler aldersovergang [behandlingId=$behandlingId, løpende behandlingId=$loependeBehandlingId]")
            val vilkaarsvurdering =
                inTransaction {
                    aldersovergangService.behandleOpphoerAldersovergang(
                        behandlingId = behandlingId,
                        loependeBehandlingId = loependeBehandlingId,
                        brukerTokenInfo = call.brukerTokenInfo,
                    )
                }

            call.respond(HttpStatusCode.OK, mapOf("vilkaarsvurderingid" to vilkaarsvurdering.id))
        }
    }
}
