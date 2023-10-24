package no.nav.etterlatte.behandling.etterbetaling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.BEHANDLINGID_CALL_PARAMETER
import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import no.nav.etterlatte.libs.common.behandlingId
import no.nav.etterlatte.libs.common.medBody
import java.time.LocalDate
import java.time.YearMonth

internal fun Route.etterbetalingRoutes(service: EtterbetalingService) {
    val logger = application.log

    route("/api/behandling/{$BEHANDLINGID_CALL_PARAMETER}/etterbetaling") {
        put {
            medBody<EtterbetalingDTO> { request ->
                logger.info("Lagrer etterbetaling for behandling $behandlingId")
                inTransaction {
                    service.lagreEtterbetaling(
                        Etterbetaling(
                            behandlingId = behandlingId,
                            fra =
                                requireNotNull(request.fraDato) { "Mangler fradato etterbetaling" }.let {
                                    YearMonth.from(
                                        it,
                                    )
                                },
                            til =
                                requireNotNull(request.tilDato) { "Mangler tildato etterbetaling" }.let {
                                    YearMonth.from(
                                        it,
                                    )
                                },
                        ),
                    )
                }
            }
            call.respond(HttpStatusCode.Created)
        }

        get {
            when (val etterbetaling = inTransaction { service.hentEtterbetaling(behandlingId) }) {
                null -> call.respond(HttpStatusCode.NoContent)
                else ->
                    call.respond(
                        HttpStatusCode.OK,
                        EtterbetalingDTO(
                            fraDato = etterbetaling.fra.atDay(1),
                            tilDato = etterbetaling.til.atEndOfMonth(),
                        ),
                    )
            }
        }

        delete {
            logger.info("Sletter etterbetaling for behandling $behandlingId")
            inTransaction { service.slettEtterbetaling(behandlingId) }
            call.respond(HttpStatusCode.OK)
        }
    }
}

data class EtterbetalingDTO(
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
)
