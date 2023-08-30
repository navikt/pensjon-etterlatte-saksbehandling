package no.nav.etterlatte.beregning.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandlingsId
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("BeregningsGrunnlag Route")

fun Route.beregningsGrunnlag(beregningsGrunnlagService: BeregningsGrunnlagService, behandlingKlient: BehandlingKlient) {
    route("/api/beregning/beregningsgrunnlag") {
        post("/{$BEHANDLINGSID_CALL_PARAMETER}/fra/{forrigeBehandlingId}") {
            val behandlingId = behandlingsId
            val forrigeBehandlingId = call.uuid("forrigeBehandlingId")

            beregningsGrunnlagService.dupliserBeregningsGrunnlagBP(behandlingId, forrigeBehandlingId)

            call.respond(HttpStatusCode.NoContent)
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/barnepensjon") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val body = call.receive<BarnepensjonBeregningsGrunnlag>()

                when {
                    beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                        behandlingId,
                        body,
                        brukerTokenInfo
                    ) -> call.respond(HttpStatusCode.NoContent)

                    else -> call.respond(HttpStatusCode.Conflict)
                }
            }
        }

        post("/{$BEHANDLINGSID_CALL_PARAMETER}/omstillingstoenad") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                val body = call.receive<OmstillingstoenadBeregningsGrunnlag>()

                when {
                    beregningsGrunnlagService.lagreOMSBeregningsGrunnlag(
                        behandlingId,
                        body,
                        brukerTokenInfo
                    ) -> call.respond(HttpStatusCode.NoContent)

                    else -> call.respond(HttpStatusCode.Conflict)
                }
            }
        }

        get("/{$BEHANDLINGSID_CALL_PARAMETER}/barnepensjon") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter grunnlag for behandling $behandlingId")
                val grunnlag = beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                    behandlingId,
                    brukerTokenInfo
                )

                when (grunnlag) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respond(HttpStatusCode.OK, grunnlag)
                }
            }
        }
    }
}

private fun ApplicationCall.uuid(param: String) = this.parameters[param]?.let {
    UUID.fromString(it)
} ?: throw NullPointerException(
    "$param er ikke i path params"
)