package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.withFoedselsnummer

fun Route.migreringRoutes(
    persondataKlient: PersondataKlient,
    behandlingKlient: BehandlingKlient,
) {
    val logger = application.log

    route("migrering") {
        post("bostedsland") {
            kunSystembruker {
                withFoedselsnummer(behandlingKlient) {
                    try {
                        val resultat = persondataKlient.hentBostedsland(it)
                        call.respond(resultat)
                    } catch (e: Exception) {
                        logger.warn("Feil fra persondata ved oppslag for vurdert bostedsland", e)
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
