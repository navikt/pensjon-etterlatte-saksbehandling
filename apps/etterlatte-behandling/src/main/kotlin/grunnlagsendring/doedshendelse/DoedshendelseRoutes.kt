package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.ktor.server.application.log
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.common.kunSystembruker
import no.nav.etterlatte.libs.common.medBody

internal fun Route.doedshendelseRoute(doedshendelseService: DoedshendelseService) {
    val logger = application.log
    route("/doedshendelse") {
        post("/brevdistribuert") {
            kunSystembruker {
                medBody<DoedshendelseBrevDistribuert> {
                    logger.info("Brev distribuert for dødshendelse med sakid: ${it.sakId} brevid: ${it.brevId}")
                    doedshendelseService.settHendelseTilFerdigOgOppdaterBrevId(it)
                }
            }
        }
    }
}
