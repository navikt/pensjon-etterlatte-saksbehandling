package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.route.routeLogger

internal fun Route.doedshendelseRoute(doedshendelseService: DoedshendelseService) {
    val logger = routeLogger
    route("/doedshendelse") {
        post("/brevdistribuert") {
            kunSystembruker {
                medBody<DoedshendelseBrevDistribuert> {
                    logger.info("Brev distribuert for dødshendelse med sakid: ${it.sakId} brevid: ${it.brevId}")
                    inTransaction {
                        doedshendelseService.settHendelseTilFerdigOgOppdaterBrevId(it)
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
