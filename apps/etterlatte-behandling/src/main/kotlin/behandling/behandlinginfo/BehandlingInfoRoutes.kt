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
import no.nav.etterlatte.libs.common.behandling.Aldersgruppe
import no.nav.etterlatte.libs.common.behandling.Brevutfall
import no.nav.etterlatte.libs.common.behandling.EtterbetalingNy
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
                medBody<BrevutfallDto> { dto ->
                    val brevutfall =
                        inTransaction {
                            logger.info("Lagrer brevutfall for behandling $behandlingId")
                            service.lagreBrevutfall(dto.toBrevutfall(behandlingId, brukerTokenInfo))
                        }
                    call.respond(brevutfall.toDto())
                }
            }

            get {
                when (val brevutfall = inTransaction { service.hentBrevutfall(behandlingId) }) {
                    null -> call.respond(HttpStatusCode.NoContent)
                    else -> call.respond(brevutfall.toDto())
                }
            }
        }

        get("/etterbetaling") {
            when (val etterbetaling = inTransaction { service.hentEtterbetaling(behandlingId) }) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respond(etterbetaling.toDto())
            }
        }
    }
}

private fun BrevutfallDto.toBrevutfall(
    behandlingId: UUID,
    bruker: BrukerTokenInfo,
): Brevutfall =
    Brevutfall(
        behandlingId = behandlingId,
        etterbetalingNy =
            if (etterbetaling?.datoFom != null && etterbetaling.datoTom != null) {
                EtterbetalingNy.fra(etterbetaling.datoFom, etterbetaling.datoTom)
            } else {
                null
            },
        aldersgruppe = aldersgruppe,
        kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
    )

private fun Brevutfall.toDto() =
    BrevutfallDto(
        behandlingId = behandlingId,
        etterbetaling = etterbetalingNy?.toDto(),
        aldersgruppe = aldersgruppe,
        kilde = kilde,
    )

private fun EtterbetalingNy.toDto() =
    EtterbetalingDto(
        datoFom = fom.atDay(1),
        datoTom = tom.atEndOfMonth(),
    )

data class BrevutfallDto(
    val behandlingId: UUID?,
    val etterbetaling: EtterbetalingDto?,
    val aldersgruppe: Aldersgruppe?,
    val kilde: Grunnlagsopplysning.Kilde?,
)

data class EtterbetalingDto(
    val datoFom: LocalDate?,
    val datoTom: LocalDate?,
)
