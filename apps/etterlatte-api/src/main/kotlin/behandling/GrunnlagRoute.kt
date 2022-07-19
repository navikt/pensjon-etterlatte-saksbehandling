package no.nav.etterlatte.behandling


import io.ktor.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route

fun Route.grunnlagRoute(service: GrunnlagService) {

    route("/grunnlag") {
        post("/kommertilgode/{behandlingId}") {

            try {
                val behandlingId = call.parameters["behandlingId"]
                val body = call.receive<KommerBarnetTilgodeClientRequest>()

                if (behandlingId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("Behandlings-id mangler")
                } else {
                    call.respond(
                        service.lagreResultatKommerBarnetTilgode(
                            behandlingId,
                            body.svar,
                            body.begrunnelse,
                            call.navIdent,
                        )
                    )
                }
            } catch (ex: Exception) {
                logger.error("kommer barnet tilgode feilet", ex)
                throw ex
            }
        }
    }
}

data class KommerBarnetTilgodeClientRequest(val svar: String, val begrunnelse: String)
