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
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.opplysningsbehov.Opplysningsbehov
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummerException
import no.nav.etterlatte.libs.common.withFoedselsnummer
import no.nav.etterlatte.libs.common.withSakId

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

        post("/person/oppdater-grunnlag") {
            kunSystembruker {
                val opplysningsbehov = call.receive<Opplysningsbehov>()
                grunnlagService.oppdaterGrunnlag(opplysningsbehov)
                call.respond(HttpStatusCode.OK)
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
            hentNavidentFraToken { navIdent ->
                try {
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
                } catch (ex: InvalidFoedselsnummerException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Gjenny har ingen navnedata på fødselsnummeret som ble etterspurt"
                    )
                } catch (ex: Exception) {
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

data class PersonMedNavn(
    val fnr: Folkeregisteridentifikator,
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?
)