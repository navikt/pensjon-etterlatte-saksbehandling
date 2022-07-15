package no.nav.etterlatte.behandling


import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.getAccessToken
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal

fun Route.grunnlagRoute(service: GrunnlagService) {

    route("/grunnlag") {
        post("/kommertilgode/{behandlingId}") {

            try {
                val behandlingId = call.parameters["behandlingId"]
                val saksbehandlerId = call.principal<TokenValidationContextPrincipal>()!!.context.getJwtToken("azure").jwtTokenClaims.getStringClaim("NAVident")
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
                            saksbehandlerId,
                            getAccessToken(call)
                        )
                    )
                }
            } catch (ex: Exception) {
                logger.error("underkjenning feilet", ex)
                throw ex
            }
        }
    }
}


data class KommerBarnetTilgodeClientRequest(val svar: String, val begrunnelse: String)
