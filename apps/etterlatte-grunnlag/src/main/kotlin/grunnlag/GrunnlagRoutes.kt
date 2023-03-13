package no.nav.etterlatte.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import no.nav.security.token.support.v2.TokenValidationContextPrincipal

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
            val opplysningstype = Opplysningstype.valueOf(call.parameters["opplysningType"].toString())
            val sakId = call.parameters["sakId"]!!.toLong()
            val grunnlag = grunnlagService.hentGrunnlagAvType(sakId, opplysningstype)

            if (grunnlag != null) {
                call.respond(grunnlag)
            } else if (opplysningstype == Opplysningstype.SOESKEN_I_BEREGNINGEN) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        post("/person") {
            val navIdent = navIdentFraToken() ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                "Kunne ikke hente ut navident for vurdering av ytelsen kommer barnet tilgode"
            )
            val body = call.receive<FoedselsnummerDTO>()
            val opplysning = grunnlagService.hentOpplysningstypeNavnFraFnr(
                Foedselsnummer.of(body.foedselsnummer),
                navIdent
            )

            if (opplysning != null) {
                call.respond(opplysning)
            } else {
                call.respond(
                    HttpStatusCode.NotFound,
                    "Doffen har ingen navnedata på fødselsnummeret som ble etterspurt"
                )
            }
        }
        post("/beregningsgrunnlag/{$BEHANDLINGSID_CALL_PARAMETER}") {
            withBehandlingId { behandlingId ->
                val body = call.receive<SoeskenMedIBeregningDTO>()
                grunnlagService.lagreSoeskenMedIBeregning(
                    behandlingId,
                    body.soeskenMedIBeregning,
                    bruker
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

internal data class FoedselsnummerDTO(
    val foedselsnummer: String
)

private data class SoeskenMedIBeregningDTO(
    val soeskenMedIBeregning: List<SoeskenMedIBeregning>
)

fun PipelineContext<Unit, ApplicationCall>.navIdentFraToken() = call.principal<TokenValidationContextPrincipal>()
    ?.context?.firstValidToken?.get()?.jwtTokenClaims?.get("NAVident")?.toString()