package no.nav.etterlatte.egenansatt

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet

internal fun Route.egenAnsattRoute(egenAnsattService: EgenAnsattService) {
    val logger = application.log

    route("/egenansatt") {
        post {
            val skjermetHendelse = call.receive<EgenAnsattSkjermet>()
            logger.info("Mottar en egen ansatt hendelse fra skjermingsløsningen")
            egenAnsattService.haandterSkjerming(skjermetHendelse)
            call.respond(HttpStatusCode.OK)
        }
    }
}