package no.nav.etterlatte.institusjonsopphold

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.hentNavidentFraToken
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

internal fun Route.institusjonsoppholdRoute(institusjonsoppholdService: InstitusjonsoppholdService) {
    route("/api/institusjonsoppholdbegrunnelse/{$SAKID_CALL_PARAMETER}") {
        post {
            kunSkrivetilgang {
                hentNavidentFraToken { navIdent ->
                    val institusjonsoppholdBegrunnelse = call.receive<InstitusjonsoppholdBegrunnelseWrapper>()
                    institusjonsoppholdService.leggInnInstitusjonsoppholdBegrunnelse(
                        sakId,
                        Grunnlagsopplysning.Saksbehandler.create(navIdent),
                        institusjonsoppholdBegrunnelse.institusjonsopphold,
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    route("/api/institusjonsoppholdbegrunnelse/{grunnlagsendringsid}") {
        get {
            val grunnlagsendringsId =
                call.parameters["grunnlagsendringsid"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Mangler grunnlagsendringsid",
                )
            val institusjonsoppholdBegrunnelseMedSaksbehandler =
                institusjonsoppholdService.hentInstitusjonsoppholdBegrunnelse(grunnlagsendringsId)
            call.respond(institusjonsoppholdBegrunnelseMedSaksbehandler ?: HttpStatusCode.NotFound)
        }
    }
}

data class InstitusjonsoppholdBegrunnelseWrapper(val institusjonsopphold: InstitusjonsoppholdBegrunnelse)
