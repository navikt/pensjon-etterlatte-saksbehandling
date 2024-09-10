package no.nav.etterlatte.institusjonsopphold

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.sakId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

internal fun Route.institusjonsoppholdRoute(institusjonsoppholdService: InstitusjonsoppholdService) {
    route("/api/institusjonsoppholdbegrunnelse/{$SAKID_CALL_PARAMETER}") {
        post {
            kunSkrivetilgang {
                val institusjonsoppholdBegrunnelse = call.receive<InstitusjonsoppholdBegrunnelseWrapper>()
                institusjonsoppholdService.leggInnInstitusjonsoppholdBegrunnelse(
                    sakId,
                    Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident()),
                    institusjonsoppholdBegrunnelse.institusjonsopphold,
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    route("/api/institusjonsoppholdbegrunnelse/{grunnlagsendringsid}") {
        get {
            val grunnlagsendringsId =
                call.parameters["grunnlagsendringsid"] ?: throw UgyldigForespoerselException(
                    "MANGLER_PARAMETER",
                    "Mangler grunnlagsendringsid",
                )
            val institusjonsoppholdBegrunnelseMedSaksbehandler =
                institusjonsoppholdService.hentInstitusjonsoppholdBegrunnelse(grunnlagsendringsId)
                    ?: throw GenerellIkkeFunnetException()
            call.respond(institusjonsoppholdBegrunnelseMedSaksbehandler)
        }
    }
}

data class InstitusjonsoppholdBegrunnelseWrapper(
    val institusjonsopphold: InstitusjonsoppholdBegrunnelse,
)
