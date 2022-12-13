package no.nav.etterlatte.libs.common

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.util.pipeline.PipelineContext
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import java.util.UUID

suspend inline fun PipelineContext<*, ApplicationCall>.withBehandlingId(onSuccess: (id: UUID) -> Unit) {
    val id = call.parameters["behandlingId"]
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, "Fant ikke behandlingId")
    }

    try {
        onSuccess(UUID.fromString(id))
    } catch (e: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest, "behandlingId må være en UUID")
    }
}

val ApplicationCall.navIdent: String get() = principal<TokenValidationContextPrincipal>()!!
    .context.getJwtToken("azure")
    .jwtTokenClaims.getStringClaim("NAVident")!!