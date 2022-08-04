package no.nav.etterlatte.behandling


import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.etterlatte.getAccessToken
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper

fun Route.grunnlagRoute(service: GrunnlagService) {

    route("/grunnlag") {
        get("{sakId}/opplysning/{opplysningType}") {
            val sakId = requireNotNull(call.parameters["sakId"])
            val opplysningType = requireNotNull(call.parameters["opplysningType"])
            val accessToken = getAccessToken(call)

            val opplysning = service.finnOpplysning(sakId, Opplysningstyper.valueOf(opplysningType), accessToken)

            call.respond(opplysning)
        }

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
                            getAccessToken(call)
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
