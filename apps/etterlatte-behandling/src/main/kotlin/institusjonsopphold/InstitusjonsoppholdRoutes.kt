package no.nav.etterlatte.institusjonsopphold

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.sakId

internal fun Route.institusjonsoppholdRoute(institusjonsoppholdService: InstitusjonsoppholdService) {
    route("/api/institusjonsoppholdbegrunnelse/{$SAKID_CALL_PARAMETER}") {
        post {
            hentNavidentFraToken { navIdent ->
                val institusjonsoppholdBegrunnelse = call.receive<InstitusjonsoppholdBegrunnelseWrapper>()
                institusjonsoppholdService.leggInnInstitusjonsoppholdBegrunnelse(
                    sakId,
                    Grunnlagsopplysning.Saksbehandler.create(navIdent),
                    institusjonsoppholdBegrunnelse.institusjonsopphold
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/api/institusjonsoppholdbegrunnelse/{grunnlagsendringsid}") {
        get {
            val tmp = institusjonsoppholdService.hentInstitusjonsoppholdBegrunnelse(
                call.parameters["grunnlagsendringsid"]!!
            )
            call.respond(tmp ?: HttpStatusCode.NotFound)
        }
    }
}

data class InstitusjonsoppholdBegrunnelseWrapper(val institusjonsopphold: InstitusjonsoppholdBegrunnelse)