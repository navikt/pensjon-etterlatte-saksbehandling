package no.nav.etterlatte.behandling.aktivitetsplikt

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.domain.TilstandException
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.hentNavidentFraToken
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

internal fun Route.aktivitetspliktRoutes(aktivitetspliktService: AktivitetspliktService) {
    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/aktivitetsplikt") {
        get {
            val result = aktivitetspliktService.hentAktivitetspliktOppfolging(behandlingId)
            call.respond(result ?: HttpStatusCode.NoContent)
        }

        post {
            kunSkrivetilgang {
                hentNavidentFraToken { navIdent ->
                    val oppfolging = call.receive<OpprettAktivitetspliktOppfolging>()

                    try {
                        val result =
                            aktivitetspliktService.lagreAktivitetspliktOppfolging(
                                behandlingId,
                                oppfolging,
                                navIdent,
                            )
                        call.respond(result)
                    } catch (e: TilstandException.UgyldigTilstand) {
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke endre på feltet")
                    }
                }
            }
        }
    }
}
