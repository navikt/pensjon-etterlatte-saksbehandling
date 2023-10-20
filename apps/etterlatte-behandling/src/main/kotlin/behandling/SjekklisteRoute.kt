package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.sjekkliste.OppdaterSjekklisteItem
import no.nav.etterlatte.behandling.sjekkliste.OppdatertSjekkliste
import no.nav.etterlatte.behandling.sjekkliste.SjekklisteService
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId

internal fun Route.sjekklisteRoute(sjekklisteService: SjekklisteService) {
    route("/api/sjekkliste/{$BEHANDLINGSID_CALL_PARAMETER}") {
        get {
            val result = sjekklisteService.hentSjekkliste(behandlingsId)
            call.respond(result ?: HttpStatusCode.NotFound)
        }

        post {
            val result = sjekklisteService.opprettSjekkliste(behandlingsId)
            call.respond(result)
        }

        put {
            val oppdatering = call.receive<OppdatertSjekkliste>()
            val result = sjekklisteService.oppdaterSjekkliste(behandlingsId, oppdatering)
            call.respond(result)
        }

        post("/item/{sjekklisteItemId}") {
            val sjekklisteItemId = requireNotNull(call.parameters["sjekklisteItemId"]).toLong()
            val oppdatering = call.receive<OppdaterSjekklisteItem>()

            val result =
                sjekklisteService.oppdaterSjekklisteItem(
                    behandlingsId,
                    sjekklisteItemId,
                    oppdatering,
                )
            call.respond(result)
        }
    }
}
