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
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.medBody
import no.nav.etterlatte.libs.ktor.brukerTokenInfo
import no.nav.etterlatte.token.BrukerTokenInfo
import java.time.LocalDate
import java.util.UUID

internal fun Route.behandlingInfoRoutes(service: BehandlingInfoService) {
    val logger = application.log

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/info") {
        route("/brevutfall") {
            post {
                medBody<BrevutfallOgEtterbetalingDto> { dto ->
                    val brevutfallOgEtterbetaling =
                        inTransaction {
                            logger.info("Lagrer brevutfall og etterbetaling for behandling $behandlingId")
                            val brevutfallLagret =
                                service.lagreBrevutfall(behandlingId, dto.toBrevutfall(behandlingId, brukerTokenInfo))
                            val etterbetalingLagret =
                                service.lagreEtterbetaling(
                                    behandlingId,
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

            get {
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
        }

        // TODO rename når den gamle er fjernet
        get("/etterbetaling-ny") {
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
): EtterbetalingNy? =
    if (etterbetaling?.datoFom != null && etterbetaling.datoTom != null) {
        EtterbetalingNy.fra(
            behandlingId = behandlingId,
            datoFom = etterbetaling.datoFom,
            datoTom = etterbetaling.datoTom,
            kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
        )
    } else {
        null
    }

private fun Brevutfall.toDto() =
    BrevutfallDto(
        behandlingId = behandlingId,
        aldersgruppe = aldersgruppe,
        kilde = kilde,
    )

private fun EtterbetalingNy.toDto() =
    EtterbetalingDto(
        behandlingId = behandlingId,
        datoFom = fom.atDay(1),
        datoTom = tom.atEndOfMonth(),
        kilde = kilde,
    )

data class BrevutfallOgEtterbetalingDto(
    val behandlingId: UUID?,
    val etterbetaling: EtterbetalingDto?,
    val brevutfall: BrevutfallDto?,
)

data class BrevutfallDto(
    val behandlingId: UUID?,
    val aldersgruppe: Aldersgruppe?,
    val kilde: Grunnlagsopplysning.Kilde?,
)

data class EtterbetalingDto(
    val behandlingId: UUID?,
    val datoFom: LocalDate?,
    val datoTom: LocalDate?,
    val kilde: Grunnlagsopplysning.Kilde?,
)
