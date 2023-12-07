package no.nav.etterlatte.behandling.brevoppsett

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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

internal fun Route.brevoppsettRoutes(service: BrevoppsettService) {
    val logger = application.log

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/brevoppsett") {
        post {
            medBody<BrevoppsettDto> { dto ->
                val brevoppsett =
                    inTransaction {
                        logger.info("Lagrer brevoppsett for behandling $behandlingId")
                        service.lagreBrevoppsett(dto.toBrevoppsett(behandlingId, brukerTokenInfo))
                    }
                call.respond(HttpStatusCode.Created, brevoppsett.toDto())
            }
        }

        put {
            medBody<BrevoppsettDto> { dto ->
                val brevoppsett =
                    inTransaction {
                        logger.info("Oppdaterer brevoppsett for behandling $behandlingId")
                        service.lagreBrevoppsett(dto.toBrevoppsett(behandlingId, brukerTokenInfo))
                    }
                call.respond(brevoppsett.toDto())
            }
        }

        get {
            when (val brevoppsett = inTransaction { service.hentBrevoppsett(behandlingId) }) {
                null -> call.respond(HttpStatusCode.NoContent)
                else -> call.respond(brevoppsett.toDto())
            }
        }
    }
}

private fun BrevoppsettDto.toBrevoppsett(
    behandlingId: UUID,
    bruker: BrukerTokenInfo,
): Brevoppsett =
    Brevoppsett(
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

private fun Brevoppsett.toDto() =
    BrevoppsettDto(
        etterbetaling =
            etterbetaling?.let {
                EtterbetalingDto(
                    datoFom = etterbetaling.fom.atDay(1),
                    datoTom = etterbetaling.tom.atEndOfMonth(),
                )
            },
        aldersgruppe = aldersgruppe,
        kilde = kilde,
    )

data class BrevoppsettDto(
    val etterbetaling: EtterbetalingDto?,
    val aldersgruppe: Aldersgruppe?,
    val kilde: Grunnlagsopplysning.Kilde?,
)

data class EtterbetalingDto(
    val datoFom: LocalDate?,
    val datoTom: LocalDate?,
)
