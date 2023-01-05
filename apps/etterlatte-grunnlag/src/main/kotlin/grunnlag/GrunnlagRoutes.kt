package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.accesstoken
import no.nav.etterlatte.libs.ktor.saksbehandler

fun Route.grunnlagRoute(grunnlagService: GrunnlagService) {
    route("grunnlag") {
        get("{sakId}") {
            val sakId = call.parameters["sakId"]!!.toLong()

            when (val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlag(sakId)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(opplysningsgrunnlag)
            }
        }

        get("{sakId}/versjon/{versjon}") {
            val sakId = call.parameters["sakId"]!!.toLong()
            val versjon = call.parameters["versjon"]!!.toLong()
            when (val opplysningsgrunnlag = grunnlagService.hentOpplysningsgrunnlagMedVersjon(sakId, versjon)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(opplysningsgrunnlag)
            }
        }

        get("{sakId}/{opplysningType}") {
            val grunnlag = grunnlagService.hentGrunnlagAvType(
                call.parameters["sakId"]!!.toLong(),
                Opplysningstype.valueOf(call.parameters["opplysningType"].toString())
            )

            if (grunnlag != null) {
                call.respond(grunnlag)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        post("/beregningsgrunnlag/{behandlingId}") {
            withBehandlingId { behandlingId ->
                val body = call.receive<SoeskenMedIBeregningDTO>()
                call.respond(
                    grunnlagService.lagreSoeskenMedIBeregning(
                        behandlingId,
                        body.soeskenMedIBeregning,
                        saksbehandler,
                        accesstoken
                    )
                )
            }
        }
    }
}

private data class SoeskenMedIBeregningDTO(
    val soeskenMedIBeregning: List<SoeskenMedIBeregning>
)