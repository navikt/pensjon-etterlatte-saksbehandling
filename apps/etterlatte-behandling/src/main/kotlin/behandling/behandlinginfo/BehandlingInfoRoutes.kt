package no.nav.etterlatte.behandling.behandlinginfo

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgEtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.EtterbetalingDto
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

internal fun Route.behandlingInfoRoutes(service: BehandlingInfoService) {
    val logger = application.log

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
                                        dto.toBrevutfall(behandlingId, brukerTokenInfo),
                                        dto.toEtterbetaling(behandlingId, brukerTokenInfo),
                                    )

                                BrevutfallOgEtterbetalingDto(
                                    behandlingId,
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

        get("/brevutfallogetterbetaling") {
            logger.info("Henter brevutfall og etterbetaling for behandling $behandlingId")
            val brevutfallOgEtterbetaling =
                inTransaction {
                    val brevutfall = service.hentBrevutfall(behandlingId)
                    val etterbetaling = service.hentEtterbetaling(behandlingId)
                    if (brevutfall != null) {
                        BrevutfallOgEtterbetalingDto(
                            behandlingId,
                            etterbetaling?.toDto(),
                            brevutfall.toDto(),
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
        kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
    )

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

private fun Brevutfall.toDto() =
    BrevutfallDto(
        behandlingId = behandlingId,
        aldersgruppe = aldersgruppe,
        kilde = kilde,
    )

private fun Etterbetaling.toDto() =
    EtterbetalingDto(
        behandlingId = behandlingId,
        datoFom = fom.atDay(1),
        datoTom = tom.atEndOfMonth(),
        kilde = kilde,
    )
