package no.nav.etterlatte.grunnlag

import grunnlag.PersonMedNavn
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.common.withSakId
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

fun Route.grunnlagRoute(grunnlagService: GrunnlagService, behandlingKlient: BehandlingKlient) {
    route("grunnlag") {
        get("{$SAKID_CALL_PARAMETER}") {
            withSakId(behandlingKlient) { sakId ->
                when (val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlag(sakId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(opplysningsgrunnlag)
                }
            }
        }

        get("{$SAKID_CALL_PARAMETER}/personer/alle") {
            withSakId(behandlingKlient) { sakId ->
                when (val personerISak = grunnlagService.hentPersonerISak(sakId)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(PersonerISakDto(personerISak))
                }
            }
        }

        get("{$SAKID_CALL_PARAMETER}/versjon/{versjon}") {
            withSakId(behandlingKlient) { sakId ->
                val versjon = call.parameters["versjon"]!!.toLong()
                when (val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlagMedVersjon(sakId, versjon)) {
                    null -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(opplysningsgrunnlag)
                }
            }
        }

        get("{$SAKID_CALL_PARAMETER}/{opplysningType}") {
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

        post("/person/saker") {
            withFoedselsnummer(behandlingKlient) { fnr ->
                val saksliste = grunnlagService.hentAlleSakerForFnr(fnr)
                call.respond(saksliste)
            }
        }

        post("/person/roller") {
            withFoedselsnummer(behandlingKlient) { fnr ->
                val personMedSakOgRoller = grunnlagService.hentSakerOgRoller(fnr)
                call.respond(personMedSakOgRoller)
            }
        }

        post("/person") {
            val navIdent = navIdentFraToken() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                "Kunne ikke hente ut navident for vurdering av ytelsen kommer barnet tilgode"
            )

            withFoedselsnummer(behandlingKlient) { foedselsnummer ->
                val opplysning = grunnlagService.hentOpplysningstypeNavnFraFnr(
                    foedselsnummer,
                    navIdent
                )

                if (opplysning != null) {
                    call.respond(opplysning)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Gjenny har ingen navnedata på fødselsnummeret som ble etterspurt"
                    )
                }
            }
        }
    }
}

private data class PersonerISakDto(
    val personer: Map<Folkeregisteridentifikator, PersonMedNavn>
)

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()