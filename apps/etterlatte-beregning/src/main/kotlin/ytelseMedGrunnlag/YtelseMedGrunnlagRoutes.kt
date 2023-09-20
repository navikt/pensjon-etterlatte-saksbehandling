package no.nav.etterlatte.ytelseMedGrunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo

fun Route.ytelseMedGrunnlag(
    ytelseMedGrunnlagService: YtelseMedGrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning/ytelse-med-grunnlag/{$BEHANDLINGSID_CALL_PARAMETER}") {
        val logger = application.log
        get {
            withBehandlingId(behandlingKlient) {
                logger.info("Henter utregnet ytelse med grunnlag for behandlingId=$it")
                when (val ytelse = ytelseMedGrunnlagService.hentYtelseMedGrunnlag(it, brukerTokenInfo)) {
                    null -> call.response.status(HttpStatusCode.NotFound)
                    else -> call.respond(ytelse)
                }
            }
        }
    }
}
