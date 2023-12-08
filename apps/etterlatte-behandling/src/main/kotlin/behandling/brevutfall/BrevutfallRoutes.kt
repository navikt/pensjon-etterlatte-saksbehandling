package no.nav.etterlatte.behandling.brevutfall

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

internal fun Route.metadataRoutes(service: BrevutfallService) {
    val logger = application.log

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}") {
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
        etterbetaling =
            if (etterbetaling?.datoFom != null && etterbetaling.datoTom != null) {
                Etterbetaling.fra(etterbetaling.datoFom, etterbetaling.datoTom)
            } else {
                null
            },
        aldersgruppe = aldersgruppe,
        kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
    )

private fun Brevutfall.toDto() =
    BrevutfallDto(
        behandlingId = behandlingId,
        etterbetaling = etterbetaling?.toDto(),
        aldersgruppe = aldersgruppe,
        kilde = kilde,
    )

private fun Etterbetaling.toDto() =
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
