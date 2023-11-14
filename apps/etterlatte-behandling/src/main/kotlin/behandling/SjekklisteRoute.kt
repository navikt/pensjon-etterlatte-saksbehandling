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
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.kunSaksbehandler

internal fun Route.sjekklisteRoute(sjekklisteService: SjekklisteService) {
    route("/api/sjekkliste/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            val result = sjekklisteService.hentSjekkliste(behandlingId)
            call.respond(result ?: HttpStatusCode.NoContent)
        }

        post {
            val result = sjekklisteService.opprettSjekkliste(behandlingId)
            call.respond(result)
        }

        put {
            kunSaksbehandler {
                val oppdatering = call.receive<OppdatertSjekkliste>()
                val result = sjekklisteService.oppdaterSjekkliste(behandlingId, oppdatering)
                call.respond(result)
            }
        }

        post("/item/{sjekklisteItemId}") {
            kunSaksbehandler {
                val sjekklisteItemId = requireNotNull(call.parameters["sjekklisteItemId"]).toLong()
                val oppdatering = call.receive<OppdaterSjekklisteItem>()

                val result =
                    sjekklisteService.oppdaterSjekklisteItem(
                        behandlingId,
                        sjekklisteItemId,
                        oppdatering,
                    )
                call.respond(result)
            }
        }
    }
}
