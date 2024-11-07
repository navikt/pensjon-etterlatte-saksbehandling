package no.nav.etterlatte.grunnlag

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.grunnlag.klienter.BehandlingKlient
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
            withFoedselsnummer(behandlingKlient, skrivetilgang = false) { fnr ->
                val personMedSakOgRoller = grunnlagService.hentSakerOgRoller(fnr)
                call.respond(personMedSakOgRoller)
            }
        }
    }
}
