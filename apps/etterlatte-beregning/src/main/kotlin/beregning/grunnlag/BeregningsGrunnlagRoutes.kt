package no.nav.etterlatte.beregning.grunnlag

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.withBehandlingId
import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("BeregningsGrunnlag Route")

fun Route.beregningsGrunnlag(
    beregningsGrunnlagService: BeregningsGrunnlagService,
    behandlingKlient: BehandlingKlient,
) {
    route("/api/beregning/beregningsgrunnlag") {
        post("/{$BEHANDLINGID_CALL_PARAMETER}/fra/{forrigeBehandlingId}") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                val forrigeBehandlingId = call.uuid("forrigeBehandlingId")

                beregningsGrunnlagService.dupliserBeregningsGrunnlag(behandlingId, forrigeBehandlingId, brukerTokenInfo)

                call.respond(HttpStatusCode.NoContent)
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/barnepensjon") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                val body = call.receive<LagreBeregningsGrunnlag>()

                when {
                    beregningsGrunnlagService.lagreBeregningsGrunnlag(
                        behandlingId,
                        body,
                        brukerTokenInfo,
                    ) -> call.respond(HttpStatusCode.NoContent)

                    else -> call.respond(HttpStatusCode.Conflict)
                }
            }
        }

        post("/{$BEHANDLINGID_CALL_PARAMETER}/omstillingstoenad") {
            withBehandlingId(behandlingKlient, skrivetilgang = true) { behandlingId ->
                val body = call.receive<LagreBeregningsGrunnlag>()

                when {
                    beregningsGrunnlagService.lagreBeregningsGrunnlag(
                        behandlingId,
                        body,
                        brukerTokenInfo,
                    ) -> call.respond(HttpStatusCode.NoContent)

                    else -> call.respond(HttpStatusCode.Conflict)
                }
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/barnepensjon") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter grunnlag for behandling $behandlingId")
                val grunnlag =
                    beregningsGrunnlagService.hentBeregningsGrunnlag(
                        behandlingId,
                        brukerTokenInfo,
                    )
                call.respond(HttpStatusCode.OK, grunnlag ?: HttpStatusCode.NoContent)
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/omstillingstoenad") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter grunnlag for behandling $behandlingId")
                val grunnlag =
                    beregningsGrunnlagService.hentBeregningsGrunnlag(
                        behandlingId,
                        brukerTokenInfo,
                    )
                call.respond(HttpStatusCode.OK, grunnlag ?: HttpStatusCode.NoContent)
            }
        }

        get("/{$BEHANDLINGID_CALL_PARAMETER}/overstyr") {
            withBehandlingId(behandlingKlient) { behandlingId ->
                logger.info("Henter overstyr grunnlag for behandling $behandlingId")

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
                logger.info("Lagre overstyr grunnlag for behandling $behandlingId")

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
                logger.info("Tilpasser overstyrt grunnlag til regulering for behandling $behandlingId")

                beregningsGrunnlagService.tilpassOverstyrtBeregningsgrunnlagForRegulering(behandlingId, brukerTokenInfo)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private fun ApplicationCall.uuid(param: String) =
    this.parameters[param]?.let {
        UUID.fromString(it)
    } ?: throw NullPointerException(
        "$param er ikke i path params",
    )
