package no.nav.etterlatte.behandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.getAccessToken
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import org.slf4j.LoggerFactory

fun Route.grunnlagRoute(service: GrunnlagService) {
    val logger = LoggerFactory.getLogger(GrunnlagService::class.java)

    route("/grunnlag") {
        post("/beregningsgrunnlag/{behandlingId}") {
            try {
                val behandlingId = call.parameters["behandlingId"]
                val body = call.receive<SoeskenMedIBeregningDTO>()

                if (behandlingId == null) { // todo ai: trekk ut
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("Behandlings-id mangler")
                } else {
                    call.respond(
                        service.lagreSoeskenMedIBeregning(
                            behandlingId,
                            body.soeskenMedIBeregning,
                            call.navIdent,
                            getAccessToken(call)
                        )
                    )
                }
            } catch (ex: Exception) {
                logger.error("beregningsgrunnlag feilet", ex)
                throw ex
            }
        }
    }
}

private data class SoeskenMedIBeregningDTO(
    val soeskenMedIBeregning: List<SoeskenMedIBeregning>
)