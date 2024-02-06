package no.nav.etterlatte.behandling.omregning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

fun Route.migreringRoutes(migreringService: MigreringService) {
    route("/migrering") {
        post {
            val migreringRequest = call.receive<MigreringRequest>()
            kunSkrivetilgang(enhetNr = migreringRequest.enhet.nr) {
                val behandling =
                    migreringService.migrer(migreringRequest).let {
                        when (it) {
                            is RetryResult.Success -> it.content
                            is RetryResult.Failure -> throw it.samlaExceptions()
                        }
                    }

                when (behandling) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else ->
                        call.respond(
                            HttpStatusCode.Companion.Created,
                            behandling,
                        )
                }
            }
        }
        post("/manuell-gjenoppretting") {
            val migreringRequest = call.receive<MigreringRequest>()
            kunSkrivetilgang(enhetNr = migreringRequest.enhet.nr) {
                migreringService.opprettOppgaveManuellGjenoppretting(migreringRequest)
                call.respond(HttpStatusCode.OK)
            }
        }

        put("/{$BEHANDLINGID_CALL_PARAMETER}/avbryt") {
            kunSkrivetilgang {
                inTransaction { migreringService.avbrytBehandling(behandlingId, brukerTokenInfo) }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
