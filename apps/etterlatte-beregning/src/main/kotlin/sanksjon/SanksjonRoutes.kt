package no.nav.etterlatte.sanksjon

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("sanksjonRoute")

const val SANKSJONID_CALL_PARAMETER = "id"
inline val PipelineContext<*, ApplicationCall>.sanksjonId: UUID
    get() =
        call.parameters[SANKSJONID_CALL_PARAMETER].let { UUID.fromString(it) } ?: throw NullPointerException(
            "Sanksjon id er ikke i path params",
        )

fun Route.sanksjon(
    sanksjonService: SanksjonService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning/sanksjon/{$BEHANDLINGID_CALL_PARAMETER}") {
        get {
            withBehandlingId(behandlingKlient, skrivetilgang = false) {
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
                val sanksjon = call.receive<LagreSanksjon>()
                sanksjonService.opprettEllerOppdaterSanksjon(it, sanksjon, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }
        }

        route("{$SANKSJONID_CALL_PARAMETER}") {
            delete {
                withBehandlingId(behandlingKlient, skrivetilgang = true) {
                    logger.info("Sletter sanksjon for behandlingId=$it med sanksjonId=$sanksjonId")
                    sanksjonService.slettSanksjon(behandlingId, sanksjonId, brukerTokenInfo)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
