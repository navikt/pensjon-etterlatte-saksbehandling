package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.withBehandlingId

fun Route.behandlingGrunnlagRoute(
    grunnlagService: GrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    /**
     * TODO:
     *  Dette blir en stegvis endring for å redusere sjansen for at alt brekker.
     *  Sak ID skal fjernes så fort vi har versjonert alt grunnlag i dev/prod med behandlingId
     **/
    route("sak/{$SAKID_CALL_PARAMETER}/behandling/{$BEHANDLINGSID_CALL_PARAMETER}") {
        get {
            withBehandlingId(behandlingKlient) { behandlingId ->
                when (val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlag(behandlingId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(opplysningsgrunnlag)
                }
            }
        }

        get("{opplysningType}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val opplysningstype = Opplysningstype.valueOf(call.parameters["opplysningType"].toString())
                val grunnlag = grunnlagService.hentGrunnlagAvType(behandlingId, opplysningstype)

                if (grunnlag != null) {
                    call.respond(grunnlag)
                } else if (opplysningstype == Opplysningstype.SOESKEN_I_BEREGNINGEN) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        get("revurdering/${Opplysningstype.HISTORISK_FORELDREANSVAR}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                when (val historisk = grunnlagService.hentHistoriskForeldreansvar(behandlingId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(historisk)
                }
            }
        }

        post("nye-opplysninger") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val opplysningsbehov = call.receive<NyeSaksopplysninger>()
                grunnlagService.lagreNyeSaksopplysninger(
                    opplysningsbehov.sakId,
                    behandlingId,
                    opplysningsbehov.opplysninger,
                )
                call.respond(HttpStatusCode.OK)
            }
        }

        post("oppdater-grunnlag") {
            kunSystembruker {
                withBehandlingId(behandlingKlient) { behandlingId ->
                    val opplysningsbehov = call.receive<Opplysningsbehov>()
                    grunnlagService.oppdaterGrunnlag(behandlingId, opplysningsbehov)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
