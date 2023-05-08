package no.nav.etterlatte.beregning.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.etterlatte.beregning.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.BEHANDLINGSID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.withBehandlingId
import no.nav.etterlatte.libs.ktor.bruker
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("BeregningsGrunnlag Route")

fun Route.beregningsGrunnlag(beregningsGrunnlagService: BeregningsGrunnlagService, behandlingKlient: BehandlingKlient) {
    post("/api/beregning/beregningsgrunnlag/{$BEHANDLINGSID_CALL_PARAMETER}/barnepensjon") {
        withBehandlingId(behandlingKlient) { behandlingId ->
            val body = call.receive<BarnepensjonBeregningsGrunnlag>()

            when {
                beregningsGrunnlagService.lagreBarnepensjonBeregningsGrunnlag(
                    behandlingId,
                    body,
                    bruker
                ) -> call.respond(HttpStatusCode.NoContent)

                else -> call.respond(HttpStatusCode.Conflict)
            }
        }
    }

    get("/api/beregning/beregningsgrunnlag/{$BEHANDLINGSID_CALL_PARAMETER}/barnepensjon") {
        withBehandlingId(behandlingKlient) { behandlingId ->
            logger.info("Henter grunnlag for behandling $behandlingId")
            val grunnlag = beregningsGrunnlagService.hentBarnepensjonBeregningsGrunnlag(
                behandlingId
            )

            when (grunnlag) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respond(HttpStatusCode.OK, grunnlag)
            }
        }
    }
}