package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.hentNavidentFraToken
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummerException
import no.nav.etterlatte.libs.common.withFoedselsnummer

fun Route.personRoute(
    grunnlagService: GrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    route("person") {
        post("saker") {
            withFoedselsnummer(behandlingKlient) { fnr ->
                val saksliste = grunnlagService.hentAlleSakerForFnr(fnr)
                call.respond(saksliste)
            }
        }

        post("roller") {
            kunSystembruker {
                withFoedselsnummer(behandlingKlient) { fnr ->
                    val personMedSakOgRoller = grunnlagService.hentSakerOgRoller(fnr)
                    call.respond(personMedSakOgRoller)
                }
            }
        }

        post("navn") {
            hentNavidentFraToken { navIdent ->
                try {
                    withFoedselsnummer(behandlingKlient) { foedselsnummer ->
                        val opplysning =
                            grunnlagService.hentOpplysningstypeNavnFraFnr(
                                foedselsnummer,
                                navIdent,
                            )

                        if (opplysning != null) {
                            call.respond(opplysning)
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                "Gjenny har ingen navnedata på fødselsnummeret som ble etterspurt",
                            )
                        }
                    }
                } catch (ex: InvalidFoedselsnummerException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Gjenny har ingen navnedata på fødselsnummeret som ble etterspurt",
                    )
                } catch (ex: Exception) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Gjenny har ingen navnedata på fødselsnummeret som ble etterspurt",
                    )
                }
            }
        }
    }
}
