package no.nav.etterlatte.beregning.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.route.uuid
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo

fun Route.beregningsGrunnlag(
    beregningsGrunnlagService: BeregningsGrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning/beregningsgrunnlag") {
        post("/{$BEHANDLINGID_CALL_PARAMETER}/fra/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")

                logger.info("Dupliserer beregningsgrunnlag for behandling $behandlingId fra $forrigeBehandlingId")
                beregningsGrunnlagService.dupliserBeregningsGrunnlag(behandlingId, forrigeBehandlingId, brukerTokenInfo)

                call.respond(HttpStatusCode.NoContent)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Lagrer beregningsgrunnlag for behandling $behandlingId")
                val body = call.receive<LagreBeregningsGrunnlag>()

                val lagretBeregningsGrunnlag =
                    beregningsGrunnlagService
                        .lagreBeregningsGrunnlag(
                            behandlingId,
                            body,
                            brukerTokenInfo,
                        )

                if (lagretBeregningsGrunnlag != null) {
                    call.respond(HttpStatusCode.OK, lagretBeregningsGrunnlag)
                } else {
                    call.respond(HttpStatusCode.Conflict)
                }
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter beregningdsgrunnlag for behandling $behandlingId")
                val grunnlag =
                    beregningsGrunnlagService.hentBeregningsGrunnlag(
                        behandlingId,
                        brukerTokenInfo,
                    )
                if (grunnlag != null) {
                    call.respond(HttpStatusCode.OK, grunnlag)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/overstyr") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter overstyrt beregningsgrunnlag for behandling $behandlingId")

                val grunnlag =
                    OverstyrBeregningGrunnlagDTO(
                        perioder =
                            beregningsGrunnlagService
                                .hentOverstyrBeregningGrunnlag(
                                    behandlingId,
                                ).perioder,
                    )

                call.respond(HttpStatusCode.OK, grunnlag)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/overstyr") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Lagrer overstyrt beregningsgrunnlag for behandling $behandlingId")

                val body = call.receive<OverstyrBeregningGrunnlagDTO>()

                val grunnlag =
                    beregningsGrunnlagService.lagreOverstyrBeregningGrunnlag(
                        behandlingId,
                        body,
                        brukerTokenInfo,
                    )

                call.respond(HttpStatusCode.OK, grunnlag)
            }
        }

        put("/{$BEHANDLINGID_CALL_PARAMETER}/overstyr/regulering") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                logger.info("Tilpasser overstyrt beregningsgrunnlag til regulering for behandling $behandlingId")

                beregningsGrunnlagService.tilpassOverstyrtBeregningsgrunnlagForRegulering(behandlingId, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
