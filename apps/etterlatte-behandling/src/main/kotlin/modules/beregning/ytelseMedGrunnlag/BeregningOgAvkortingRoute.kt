package no.nav.etterlatte.modules.beregning.ytelseMedGrunnlag

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import org.slf4j.LoggerFactory

fun Route.ytelseMedGrunnlag(
    beregningOgAvkortingBrevService: BeregningOgAvkortingBrevService,
    behandlingTilgangsSjekk: BehandlingTilgangsSjekk,
) {
    route("/api/beregning/ytelse-med-grunnlag/{$BEHANDLINGID_CALL_PARAMETER}") {
        val logger = LoggerFactory.getLogger("YtelseMedGrunnlagRoute")
        get {
            withBehandlingId(behandlingTilgangsSjekk) {
                logger.info("Henter utregnet ytelse med grunnlag for behandlingId=$it")
                val ytelse =
                    beregningOgAvkortingBrevService.hentBeregningOgAvkorting(it, brukerTokenInfo)
                        ?: throw GenerellIkkeFunnetException()
                call.respond(ytelse)
            }
        }
    }
}
