package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummerException
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.routeLogger
import no.nav.etterlatte.libs.ktor.route.withFoedselsnummer

fun Route.personRoute(
    grunnlagService: GrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    route("/person") {
        post("/saker") {
            withFoedselsnummer(behandlingKlient, skrivetilgang = false) { fnr ->
                val saksliste = grunnlagService.hentAlleSakerForFnr(fnr)
                call.respond(saksliste)
            }
        }

        post("/roller") {
            kunSystembruker {
                withFoedselsnummer(behandlingKlient, skrivetilgang = false) { fnr ->
                    val personMedSakOgRoller = grunnlagService.hentSakerOgRoller(fnr)
                    call.respond(personMedSakOgRoller)
                }
            }
        }
        // TODO: brukes denne lenger?
        post("/navn") {
            try {
                withFoedselsnummer(behandlingKlient, skrivetilgang = false) { foedselsnummer ->
                    val opplysning =
                        grunnlagService.hentOpplysningstypeNavnFraFnr(
                            foedselsnummer,
                            brukerTokenInfo.ident(),
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
                routeLogger.error("Fikk feilmelding under henting av navn fra grunnlag", ex)
                call.respond(
                    HttpStatusCode.NotFound,
                    "Gjenny har ingen navnedata på fødselsnummeret som ble etterspurt",
                )
            }
        }

        post("vergeadresse") {
            withFoedselsnummer(behandlingKlient) { fnr ->
                when (val adresse = grunnlagService.hentVergeadresse(fnr.value)) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respond(adresse)
                }
            }
        }
    }
}
