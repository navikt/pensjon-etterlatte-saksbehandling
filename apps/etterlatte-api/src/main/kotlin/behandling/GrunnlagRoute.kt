package no.nav.etterlatte.behandling

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.getAccessToken
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.navIdent
import no.nav.etterlatte.libs.common.withBehandlingId
import org.slf4j.LoggerFactory

fun Route.grunnlagRoute(service: GrunnlagService) {
    val logger = LoggerFactory.getLogger(GrunnlagService::class.java)

    route("/grunnlag") {
        post("/beregningsgrunnlag/{behandlingId}") {
            withBehandlingId { behandlingId ->
                try {
                    val body = call.receive<SoeskenMedIBeregningDTO>()
                    call.respond(
                        service.lagreSoeskenMedIBeregning(
                            behandlingId.toString(),
                            body.soeskenMedIBeregning,
                            call.navIdent,
                            getAccessToken(call)
                        )
                    )
                } catch (ex: Exception) {
                    logger.error("beregningsgrunnlag feilet", ex)
                    throw ex
                }
            }
        }
    }
}

private data class SoeskenMedIBeregningDTO(
    val soeskenMedIBeregning: List<SoeskenMedIBeregning>
)