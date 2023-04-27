package no.nav.etterlatte.vilkaarsvurdering.migrering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.vilkaarsvurdering.Utfall
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.vilkaarsvurdering.klienter.BehandlingKlient

fun Route.migrering(migreringService: MigreringService, behandlingKlient: BehandlingKlient) {
    route("/api/vilkaarsvurdering/migrering") {
        val logger = application.log

        patch("/{$BEHANDLINGSID_CALL_PARAMETER}/vilkaar/utfall/{utfall}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val utfall = call.parameters["utfall"]
                    ?.let { Utfall.valueOf(it) }
                    ?: throw IllegalArgumentException("Utfall mangler for behandling $behandlingId")
                logger.info("Setter alle vilkår til $utfall for behandling $behandlingId")
                migreringService.endreUtfallForAlleVilkaar(behandlingId, utfall)
                call.respond(HttpStatusCode.Accepted)
            }
        }
    }
}