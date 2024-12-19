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
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgEtterbetalingDto
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
                    medBody<BrevutfallOgEtterbetalingDto> { dto ->
                        val brevutfallOgEtterbetaling =
                            inTransaction {
                                logger.info("Lagrer brevutfall og etterbetaling for behandling $behandlingId")
                                val (brevutfallLagret, etterbetalingLagret) =
                                    service.lagreBrevutfallOgEtterbetaling(
                                        behandlingId,
                                        brukerTokenInfo,
                                        dto.opphoer ?: throw OpphoerIkkeSatt(behandlingId),
                                        dto.toBrevutfall(behandlingId, brukerTokenInfo),
                                        dto.toEtterbetaling(behandlingId, brukerTokenInfo),
                                    )

                                BrevutfallOgEtterbetalingDto(
                                    behandlingId,
                                    dto.opphoer,
                                    etterbetalingLagret?.toDto(),
                                    brevutfallLagret.toDto(),
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

        get("/brevutfallogetterbetaling") {
            logger.info("Henter brevutfall og etterbetaling for behandling $behandlingId")
            val brevutfallOgEtterbetaling =
                inTransaction {
                    val brevutfall = service.hentBrevutfall(behandlingId)
                    val etterbetaling = service.hentEtterbetaling(behandlingId)
                    if (brevutfall != null) {
                        BrevutfallOgEtterbetalingDto(
                            behandlingId = behandlingId,
                            etterbetaling = etterbetaling?.toDto(),
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

private fun BrevutfallOgEtterbetalingDto.toBrevutfall(
    behandlingId: UUID,
    bruker: BrukerTokenInfo,
): Brevutfall =
    Brevutfall(
        behandlingId = behandlingId,
        aldersgruppe = brevutfall?.aldersgruppe,
        feilutbetaling = brevutfall?.feilutbetaling,
        frivilligSkattetrekk = brevutfall?.frivilligSkattetrekk,
        kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
    )

private fun BrevutfallOgEtterbetalingDto.toEtterbetaling(
    behandlingId: UUID,
    bruker: BrukerTokenInfo,
): Etterbetaling? =
    Etterbetaling.fra(
        behandlingId = behandlingId,
        frivilligSkattetrekk = etterbetaling?.frivilligSkattetrekk,
        kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
    )

private fun Brevutfall.toDto() =
    BrevutfallDto(
        behandlingId = behandlingId,
        aldersgruppe = aldersgruppe,
        feilutbetaling = feilutbetaling,
        frivilligSkattetrekk = frivilligSkattetrekk,
        kilde = kilde,
    )

private fun Etterbetaling.toDto() =
    EtterbetalingDto(
        behandlingId = behandlingId,
        kilde = kilde,
        frivilligSkattetrekk = frivilligSkattetrekk,
    )

class OpphoerIkkeSatt(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "OPPHOER_IKKE_SATT",
        detail = "Behandling $behandlingId har ikke angitt om det er opphoer.",
    )
