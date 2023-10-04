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
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sakId
import no.nav.etterlatte.libs.common.withSakId

fun Route.grunnlagRoute(
    grunnlagService: GrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    route("sak/{$SAKID_CALL_PARAMETER}") {
        get {
            withSakId(behandlingKlient) { sakId ->
                when (val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlag(sakId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(opplysningsgrunnlag)
                }
            }
        }

        get("personer/alle") {
            withSakId(behandlingKlient) { sakId ->
                when (val personerISak = grunnlagService.hentPersonerISak(sakId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(PersonerISakDto(personerISak))
                }
            }
        }

        get("{opplysningType}") {
            withSakId(behandlingKlient) { sakId ->
                val opplysningstype = Opplysningstype.valueOf(call.parameters["opplysningType"].toString())
                val grunnlag = grunnlagService.hentGrunnlagAvType(sakId, opplysningstype)

                if (grunnlag != null) {
                    call.respond(grunnlag)
                } else if (opplysningstype == Opplysningstype.SOESKEN_I_BEREGNINGEN) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        get("revurdering/${Opplysningstype.HISTORISK_FORELDREANSVAR.name}") {
            withSakId(behandlingKlient) { sakId ->
                when (val historisk = grunnlagService.hentHistoriskForeldreansvar(sakId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(historisk)
                }
            }
        }

        post("nye-opplysninger") {
            withSakId(behandlingKlient) {
                val opplysningsbehov = call.receive<NyeSaksopplysninger>()
                grunnlagService.lagreNyeSaksopplysninger(sakId, opplysningsbehov.opplysninger)
                call.respond(HttpStatusCode.OK)
            }
        }

        post("oppdater-grunnlag") {
            kunSystembruker {
                withSakId(behandlingKlient) {
                    val opplysningsbehov = call.receive<Opplysningsbehov>()
                    grunnlagService.oppdaterGrunnlag(opplysningsbehov)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

private data class PersonerISakDto(
    val personer: Map<Folkeregisteridentifikator, PersonMedNavn>,
)

data class PersonMedNavn(
    val fnr: Folkeregisteridentifikator,
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
)
