package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.withSakId

fun Route.sakGrunnlagRoute(
    grunnlagService: GrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    route("sak/{$SAKID_CALL_PARAMETER}") {
        get {
            withSakId(behandlingKlient) { sakId ->
                val opplysningsgrunnlag =
                    grunnlagService.hentOpplysningsgrunnlagForSak(sakId)
                        ?: throw GenerellIkkeFunnetException()
                call.respond(opplysningsgrunnlag)
            }
        }

        get("/grunnlag-finnes") {
            withSakId(behandlingKlient) { sakId ->
                val grunnlagFinnes = grunnlagService.grunnlagFinnesForSak(sakId)
                call.respond(grunnlagFinnes)
            }
        }

        post("opprett-grunnlag") {
            withSakId(behandlingKlient, skrivetilgang = true) { sakId ->
                val opplysningsbehov = call.receive<Opplysningsbehov>()
                grunnlagService.opprettEllerOppdaterGrunnlagForSak(sakId, opplysningsbehov)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/oppdater-grunnlag") {
            withSakId(behandlingKlient, skrivetilgang = true) {
                val request = call.receive<OppdaterGrunnlagRequest>()
                grunnlagService.oppdaterGrunnlagForSak(request)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("/nye-opplysninger") {
            withSakId(behandlingKlient, skrivetilgang = true) {
                val opplysningsbehov = call.receive<NyeSaksopplysninger>()
                grunnlagService.lagreNyeSaksopplysningerBareSak(
                    opplysningsbehov.sakId,
                    opplysningsbehov.opplysninger,
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

data class PersonMedNavn(
    val fnr: Folkeregisteridentifikator,
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
)
