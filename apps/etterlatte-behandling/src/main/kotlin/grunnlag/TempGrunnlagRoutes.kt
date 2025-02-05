package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.grunnlag.NyePersonopplysninger
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

fun Route.tempGrunnlagRoutes(grunnlagKlient: TempGrunnlagKlient) {
    route("/grunnlag") {
        route("/behandling/{$BEHANDLINGID_CALL_PARAMETER}") {
            post("/laas") {
                grunnlagKlient.laasVersjonForBehandling(behandlingId, brukerTokenInfo)
                call.respond(HttpStatusCode.OK)
            }

            post("/nye-opplysninger") {
                val opplysningsbehov = call.receive<NyeSaksopplysninger>()

                grunnlagKlient.lagreNyeSaksopplysninger(
                    opplysningsbehov.sakId,
                    behandlingId,
                    opplysningsbehov.opplysninger,
                    brukerTokenInfo,
                )
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/person/behandling/{$BEHANDLINGID_CALL_PARAMETER}/nye-opplysninger") {
            val nyePersonopplysninger = call.receive<NyePersonopplysninger>()

            grunnlagKlient.lagreNyePersonopplysninger(
                sakId = nyePersonopplysninger.sakId,
                behandlingId = behandlingId,
                fnr = nyePersonopplysninger.fnr,
                nyeOpplysninger = nyePersonopplysninger.opplysninger,
                brukerTokenInfo = brukerTokenInfo,
            )

            call.respond(HttpStatusCode.OK)
        }
    }
}
