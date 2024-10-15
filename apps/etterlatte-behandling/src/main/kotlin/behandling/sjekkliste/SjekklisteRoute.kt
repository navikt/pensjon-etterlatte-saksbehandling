package no.nav.etterlatte.behandling.sjekkliste

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSaksbehandlerMedSkrivetilgang
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

internal fun Route.sjekklisteRoute(sjekklisteService: SjekklisteService) {
    route("/api/sjekkliste/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            val result = sjekklisteService.hentSjekkliste(behandlingId)
            call.respond(result ?: HttpStatusCode.NoContent)
        }

        post {
            kunSkrivetilgang {
                val result = sjekklisteService.opprettSjekkliste(behandlingId, brukerTokenInfo)
                call.respond(result)
            }
        }

        put {
            kunSaksbehandlerMedSkrivetilgang {
                val oppdatering = call.receive<OppdatertSjekkliste>()
                val result = sjekklisteService.oppdaterSjekkliste(behandlingId, oppdatering)
                call.respond(result)
            }
        }

        post("/item/{sjekklisteItemId}") {
            kunSaksbehandlerMedSkrivetilgang {
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
