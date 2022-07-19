package no.nav.etterlatte.behandling

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("no.nav.etterlatte.behandling.VedtakRoute")
fun Route.vedtakRoute(service: VedtakService) {

    route("fattvedtak") {
        post("{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Behandlings-id mangler")
            } else {
                call.respond(service.fattVedtak(behandlingId, call.navIdent))
            }
        }
    }
    route("attestervedtak") {
        post("{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Behandlings-id mangler")
            } else {
                call.respond(service.attesterVedtak(behandlingId, call.navIdent))
            }
        }
    }

    route("underkjennvedtak") {
        post("{behandlingId}") {
            try {
                val behandlingId = call.parameters["behandlingId"]
                logger.info("Skal underkjenne vedtak i behandling $behandlingId")

                val body = call.receive<UnderkjennVedtakClientRequest>()

                if (behandlingId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("Behandlings-id mangler")
                } else {
                    call.respond(
                        service.underkjennVedtak(
                            behandlingId,
                            body.valgtBegrunnelse,
                            body.kommentar,
                            call.navIdent
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

val ApplicationCall.navIdent: String get() = principal<TokenValidationContextPrincipal>()!!.context.getJwtToken("azure").jwtTokenClaims.getStringClaim("NAVident")!!
data class UnderkjennVedtakClientRequest(val kommentar: String, val valgtBegrunnelse: String)
