package no.nav.etterlatte

import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import no.nav.security.token.support.ktor.TokenValidationContextPrincipal
import java.util.*

data class FattVedtakBody(val sakId: String, val behandlingId: String)

fun Route.Api(service: VedtaksvurderingService) {
    get("hentvedtak/{sakId}/{behandlingId}") {
        val sakId = call.parameters["sakId"].toString()
        val behandlingId = UUID.fromString(call.parameters["behandlingId"])
        val vedtaksresultat = service.hentVedtak(sakId, behandlingId)
        if(vedtaksresultat == null) {
            call.response.status(HttpStatusCode.NotFound)
        } else {
            call.respond(vedtaksresultat)
        }
    }


    post("fattVedtak") {
        val saksbehandlerId = call.principal<TokenValidationContextPrincipal>()!!.context.getJwtToken("azure").jwtTokenClaims.getStringClaim("NAVident")

        val vedtakBody = call.receive<FattVedtakBody>()
        service.fattVedtak(vedtakBody.sakId, UUID.fromString(vedtakBody.behandlingId), saksbehandlerId)
        call.respond("ok")
    }
    post("attesterVedtak") {
        val saksbehandlerId = call.principal<TokenValidationContextPrincipal>()!!.context.getJwtToken("azure").jwtTokenClaims.getStringClaim("NAVident")
        val vedtakBody = call.receive<FattVedtakBody>()
        service.attesterVedtak(vedtakBody.sakId, UUID.fromString(vedtakBody.behandlingId), saksbehandlerId)
        call.respond("ok")
    }
}