package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.withSakId

fun Route.sakGrunnlagRoute(
    grunnlagService: GrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    route("sak/{$SAKID_CALL_PARAMETER}") {
        get {
            withSakId(behandlingKlient) { sakId ->
                when (val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlagForSak(sakId)) {
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
    }
}

data class PersonerISakDto(
    val personer: Map<Folkeregisteridentifikator, PersonMedNavn>,
)

data class PersonMedNavn(
    val fnr: Folkeregisteridentifikator,
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
)
