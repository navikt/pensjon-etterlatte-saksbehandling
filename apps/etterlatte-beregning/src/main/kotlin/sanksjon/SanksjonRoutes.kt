package no.nav.etterlatte.sanksjon

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.withBehandlingId

fun Route.sanksjon(
    sanksjonService: SanksjonService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning/sanksjon/{$BEHANDLINGID_CALL_PARAMETER}") {
        val logger = application.log

        get {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Henter sanksjoner for behandlingId=$it")
                val sanksjoner = sanksjonService.hentSanksjon(it)
                when (sanksjoner) {
                    null -> call.response.status(HttpStatusCode.NoContent)
                    else -> call.respond(sanksjoner)
                }
            }
        }

        post {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Oppdaterer eller oppretter sanksjon for behandlingId=$it")
                val sanksjon = call.receive<Sanksjon>()
                sanksjonService.opprettEllerOppdaterSanksjon(it, sanksjon, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }
        }

        delete {
            withBehandlingId(behandlingKlient, skrivetilgang = true) {
                logger.info("Sletter sanksjon for behandlingId=$it")
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
