package no.nav.etterlatte.behandling.behandlinginfo

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.utland.SluttbehandlingUtlandBehandlinginfoRequest
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgInfo
import no.nav.etterlatte.libs.common.behandling.EtterbetalingDto
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.ktor.route.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.route.behandlingId
import no.nav.etterlatte.libs.ktor.route.medBody
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import no.nav.etterlatte.tilgangsstyring.kunSkrivetilgang
import org.slf4j.LoggerFactory
import java.util.UUID

internal fun Route.behandlingInfoRoutes(service: BehandlingInfoService) {
    val logger = LoggerFactory.getLogger("BehandlingsInfoRoute")

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/info") {
        route("/brevutfall") {
            post {
                kunSkrivetilgang {
                    medBody<BrevutfallOgInfo> { request ->
                        val brevutfallOgEtterbetaling =
                            inTransaction {
                                logger.info("Lagrer brevutfall for behandling $behandlingId")

                                BrevutfallOgInfo(
                                    behandlingId,
                                    service
                                        .lagreBrevutfall(
                                            behandlingId,
                                            brukerTokenInfo,
                                            request.brevutfall?.opphoer ?: throw OpphoerIkkeSatt(behandlingId),
                                            request.toBrevutfall(behandlingId, brukerTokenInfo),
                                        ).toDto(),
                                )
                            }
                        call.respond(brevutfallOgEtterbetaling)
                    }
                }
            }

            get {
                logger.info("Henter brevutfall for behandling $behandlingId")
                val brevutfall =
                    inTransaction {
                        service.hentBrevutfall(behandlingId)
                    }
                when (brevutfall) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respond(brevutfall.toDto())
                }
            }
        }

        route("sluttbehandling") {
            post {
                kunSkrivetilgang {
                    medBody<SluttbehandlingUtlandBehandlinginfoRequest> { dto ->
                        logger.info("Lagrer sluttbehandling for behandling $behandlingId")
                        inTransaction {
                            service.lagreSluttbehandling(
                                behandlingId,
                                dto.toDomain(Grunnlagsopplysning.Saksbehandler.create(brukerTokenInfo.ident())),
                            )
                        }
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            get {
                logger.info("Henter sluttbehandling for behandling $behandlingId")
                val sluttbehandling = inTransaction { service.hentSluttbehandling(behandlingId) }
                call.respond(sluttbehandling ?: HttpStatusCode.NoContent)
            }
        }

        get("/brevutfall") {
            logger.info("Henter brevutfall for behandling $behandlingId")
            val brevutfallOgEtterbetaling =
                inTransaction {
                    val brevutfall = service.hentBrevutfall(behandlingId)
                    if (brevutfall != null) {
                        BrevutfallOgInfo(
                            behandlingId = behandlingId,
                            brevutfall = brevutfall.toDto(),
                        )
                    } else {
                        null
                    }
                }
            when (brevutfallOgEtterbetaling) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respond(brevutfallOgEtterbetaling)
            }
        }

        get("/etterbetaling") {
            logger.info("Henter etterbetaling for behandling $behandlingId")
            when (val etterbetaling = inTransaction { service.hentEtterbetaling(behandlingId) }) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respond(etterbetaling.toDto())
            }
        }
    }
}

private fun BrevutfallOgInfo.toBrevutfall(
    behandlingId: UUID,
    bruker: BrukerTokenInfo,
): Brevutfall =
    Brevutfall(
        behandlingId = behandlingId,
        aldersgruppe = brevutfall?.aldersgruppe,
        feilutbetaling = brevutfall?.feilutbetaling,
        frivilligSkattetrekk = brevutfall?.frivilligSkattetrekk,
        kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
        harEtterbetaling = brevutfall?.harEtterbetaling,
    )

private fun Brevutfall.toDto() =
    BrevutfallDto(
        behandlingId = behandlingId,
        aldersgruppe = aldersgruppe,
        feilutbetaling = feilutbetaling,
        frivilligSkattetrekk = frivilligSkattetrekk,
        kilde = kilde,
        harEtterbetaling = harEtterbetaling,
    )

private fun Etterbetaling.toDto() =
    EtterbetalingDto(
        behandlingId = behandlingId,
        datoFom = fom.atDay(1),
        datoTom = tom.atEndOfMonth(),
        kilde = kilde,
        inneholderKrav = inneholderKrav,
        frivilligSkattetrekk = frivilligSkattetrekk,
        etterbetalingPeriodeValg = etterbetalingPeriodeValg,
    )

class OpphoerIkkeSatt(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "OPPHOER_IKKE_SATT",
        detail = "Behandling $behandlingId har ikke angitt om det er opphoer.",
    )
