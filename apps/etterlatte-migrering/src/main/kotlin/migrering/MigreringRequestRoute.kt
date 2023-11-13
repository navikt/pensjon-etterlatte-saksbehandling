package migrering

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.migrering.PesysRepository
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MigreringRequestRoute")

internal fun Route.migreringRequestRoute(pesysRepository: PesysRepository) {
    route("behandling") {
        get("{$BEHANDLINGID_CALL_PARAMETER}") {
            // Skipper oppsett med tilgangssjekking mot behandling, da dette endepunktet kun er åpent for
            // systembrukere (som alltid har tilgang)
            kunSystembruker {
                logger.info("Henter migreringrequest for behandling med id=$behandlingId")
                when (val sak = pesysRepository.hentPesyssakForBehandlingId(behandlingId)) {
                    null -> throw GenerellIkkeFunnetException()
                    else -> call.respond(sak.tilMigreringsrequest())
                }
            }
        }
    }
}
