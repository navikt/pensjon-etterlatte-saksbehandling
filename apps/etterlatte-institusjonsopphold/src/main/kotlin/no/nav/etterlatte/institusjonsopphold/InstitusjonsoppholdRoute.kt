package no.nav.etterlatte.institusjonsopphold

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.institusjonsopphold.klienter.HentOppholdRequest
import no.nav.etterlatte.institusjonsopphold.klienter.InstitusjonsoppholdKlient
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody

fun Route.institusjonsoppholdRoute(institusjonsoppholdKlient: InstitusjonsoppholdKlient) {
    route("api") {
        route("personer/institusjonsopphold") {
            post {
                kunSystembruker {
                    medBody<HentOppholdRequest> {
                        val oppholdForPersoner = institusjonsoppholdKlient.hentOppholdForPersoner(it)
                        call.respond(HttpStatusCode.OK, oppholdForPersoner)
                    }
                }
            }
        }
    }
}
