package no.nav.etterlatte.egenansatt

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.BehandlingRequestLogger
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

internal fun Route.egenAnsattRoute(
    egenAnsattService: EgenAnsattService,
    requestLogger: BehandlingRequestLogger,
) {
    val logger = LoggerFactory.getLogger("EgenAnsattRoute")

    route("/egenansatt") {
        post {
            kunSystembruker {
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
