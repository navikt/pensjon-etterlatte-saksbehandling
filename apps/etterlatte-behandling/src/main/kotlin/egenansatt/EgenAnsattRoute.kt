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
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang

internal fun Route.egenAnsattRoute(
    egenAnsattService: EgenAnsattService,
    requestLogger: BehandlingRequestLogger,
) {
    val logger = application.log

    route("/egenansatt") {
        post {
            kunSkrivetilgang {
                val skjermetHendelse = call.receive<EgenAnsattSkjermet>()
                logger.info("Mottar en egen ansatt hendelse fra skjermingsløsningen")
                inTransaction {
                    egenAnsattService.haandterSkjerming(skjermetHendelse)
                }.also {
                    requestLogger.loggRequest(
                        brukerTokenInfo,
                        Folkeregisteridentifikator.of(skjermetHendelse.fnr),
                        "egenansatt-skjermet",
                    )
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
