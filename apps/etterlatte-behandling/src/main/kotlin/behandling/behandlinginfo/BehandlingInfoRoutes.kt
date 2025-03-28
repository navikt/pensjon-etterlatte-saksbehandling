package no.nav.etterlatte.behandling.behandlinginfo

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.behandling.utland.SluttbehandlingBehandlinginfoRequest
import no.nav.etterlatte.inTransaction
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
                    medBody<BrevutfallOgEtterbetalingDto> { brevutfallDto ->
                        val brevutfallOgEtterbetaling =
                            inTransaction {
                                logger.info("Lagrer brevutfall og etterbetaling for behandling $behandlingId")
                                val (brevutfallLagret, etterbetalingLagret) =
                                    service.lagreBrevutfallOgEtterbetaling(
                                        behandlingId,
                                        brukerTokenInfo,
                                        brevutfallDto.opphoer ?: throw OpphoerIkkeSatt(behandlingId),
                                        brevutfallDto.brevutfall!!,
                                        brevutfallDto.toEtterbetaling(behandlingId, brukerTokenInfo),
                                    )

                                BrevutfallOgEtterbetalingDto(
                                    behandlingId,
                                    brevutfallDto.opphoer,
                                    etterbetalingLagret?.toDto(),
                                    brevutfallLagret,
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
                    else -> call.respond(brevutfall)
                }
            }
        }

        route("sluttbehandling") {
            post {
                kunSkrivetilgang {
                    medBody<SluttbehandlingBehandlinginfoRequest> { dto ->
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
                            brevutfall = brevutfall,
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

private fun BrevutfallOgEtterbetalingDto.toEtterbetaling(
    behandlingId: UUID,
    bruker: BrukerTokenInfo,
): Etterbetaling? {
    val etterbetalingCopy = etterbetaling
    return if (etterbetalingCopy?.datoFom != null && etterbetalingCopy.datoTom != null) {
        Etterbetaling.fra(
            behandlingId = behandlingId,
            datoFom = etterbetalingCopy.datoFom,
            datoTom = etterbetalingCopy.datoTom,
            kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
        )
    } else {
        null
    }
}

private fun Etterbetaling.toDto() =
    EtterbetalingDto(
        behandlingId = behandlingId,
        datoFom = fom.atDay(1),
        datoTom = tom.atEndOfMonth(),
        kilde = kilde,
    )

class OpphoerIkkeSatt(
    behandlingId: UUID,
) : UgyldigForespoerselException(
        code = "OPPHOER_IKKE_SATT",
        detail = "Behandling $behandlingId har ikke angitt om det er opphoer.",
    )
