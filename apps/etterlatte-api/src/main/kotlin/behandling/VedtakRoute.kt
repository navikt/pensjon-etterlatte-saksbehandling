package no.nav.etterlatte.behandling


import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
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
