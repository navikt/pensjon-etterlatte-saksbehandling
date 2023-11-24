package migrering

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("migreringRoute")

internal fun Route.migreringRoute(pesysRepository: PesysRepository) {
    route("migrering") {
        post("{$BEHANDLINGID_CALL_PARAMETER}") {
            kunSystembruker {
                val pesysid = call.receive<Long>()
                logger.info("Oppretter manuell migrering for pesys sak $pesysid og behandling $behandlingId")
                pesysRepository.lagreManuellMigrering(pesysid)
                pesysRepository.lagreKoplingTilBehandling(behandlingId, PesysId(pesysid))
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
